package com.rootlink.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rootlink.backend.entity.User;
import com.rootlink.backend.entity.UserProfile;
import com.rootlink.backend.entity.UserRelation;
import com.rootlink.backend.entity.UserRelationApply;
import com.rootlink.backend.exception.BusinessException;
import com.rootlink.backend.mapper.UserMapper;
import com.rootlink.backend.mapper.UserProfileMapper;
import com.rootlink.backend.mapper.UserRelationApplyMapper;
import com.rootlink.backend.mapper.UserRelationMapper;
import com.rootlink.backend.utils.NebulaRelationResolver;
import com.rootlink.backend.utils.NebulaUtil;
import com.rootlink.backend.utils.RelationInferenceUtil;
import com.rootlink.backend.utils.SecurityUtil;
import com.vesoft.nebula.client.graph.data.PathWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 亲属关系服务
 *
 * 架构：
 *   MySQL  → 存储用户、确认关系、申请记录（source of truth）
 *   Nebula → 存储图结构，用于多跳推断
 *
 * 流程：
 *   1. 申请 → MySQL 存 apply
 *   2. 同意 → MySQL 存 user_relation（confirmed）+ Nebula 写边
 *   3. 推断 → Nebula FIND ALL PATH → NebulaRelationResolver 解析称谓
 *            → MySQL 存 user_relation（infer_status=1 待确认）
 *   4. Nebula 不可用时，自动 fallback 到旧 Java 链式推断逻辑
 */
@Slf4j
@Service
public class RelationService {

    @Autowired private UserMapper userMapper;
    @Autowired private UserRelationMapper relationMapper;
    @Autowired private UserRelationApplyMapper applyMapper;
    @Autowired private UserProfileMapper userProfileMapper;
    @Autowired private SecurityUtil securityUtil;
    @Autowired private RelationInferenceUtil inferUtil;   // 保留，用于 fallback 及链解析
    @Autowired private NebulaUtil nebulaUtil;
    @Autowired private NebulaRelationResolver nebulaResolver;

    // ═══════════════════════════════════════════════════
    // 搜索用户
    // ═══════════════════════════════════════════════════

    public Map<String, Object> searchUser(Long currentUserId, String idCard) {
        if (idCard == null || idCard.isBlank())
            throw new BusinessException(400, "请输入身份证号");
        String hash = securityUtil.sha256Hash(idCard.trim());
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.eq(User::getIdCardHash, hash)
         .eq(User::getAllowSearch, true)
         .eq(User::getStatus, 1)
         .ne(User::getId, currentUserId)
         .eq(User::getDeleted, 0);
        User user = userMapper.selectOne(w);
        if (user == null) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("realName", user.getRealName() != null ? user.getRealName() : "已实名用户");
        result.put("phone", maskPhone(user.getPhone()));
        result.put("realNameStatus", user.getRealNameStatus());
        result.put("lifeStatus", user.getLifeStatus());
        result.put("relationStatus", getRelationStatus(currentUserId, user.getId()));
        return result;
    }

    // ═══════════════════════════════════════════════════
    // 申请关系
    // ═══════════════════════════════════════════════════

    @Transactional(rollbackFor = Exception.class)
    public void applyRelation(Long applicantId, Long targetUserId, String relationChainJson, String reason) {
        User target = userMapper.selectById(targetUserId);
        if (target == null || target.getDeleted() == 1)
            throw new BusinessException(404, "目标用户不存在");
        if (applicantId.equals(targetUserId))
            throw new BusinessException(400, "不能与自己建立关系");
        if ("related".equals(getRelationStatus(applicantId, targetUserId)))
            throw new BusinessException(400, "已存在亲属关系");
        if ("pending".equals(getRelationStatus(applicantId, targetUserId)))
            throw new BusinessException(400, "已有待处理的申请");

        List<String> chain = inferUtil.jsonToChain(relationChainJson);
        String relationName = inferUtil.resolveChain(chain);

        UserRelationApply apply = new UserRelationApply();
        apply.setApplicantUserId(applicantId);
        apply.setTargetUserId(targetUserId);
        apply.setRelationType(chainToType(chain));
        apply.setRelationDesc(relationName);
        // 把链存入 reason 字段（前缀法，兼容原有字段）
        apply.setReason("[CHAIN]" + relationChainJson + (reason != null ? " " + reason : ""));
        apply.setApplyStatus(0);
        applyMapper.insert(apply);
        log.info("关系申请: applicantId={}, targetId={}, chain={}", applicantId, targetUserId, relationChainJson);
    }

    // ═══════════════════════════════════════════════════
    // 处理申请（核心：同意时写 MySQL + Nebula）
    // ═══════════════════════════════════════════════════

    @Transactional(rollbackFor = Exception.class)
    public void handleApply(Long currentUserId, Long applyId, Integer action, String rejectReason) {
        UserRelationApply apply = applyMapper.selectById(applyId);
        if (apply == null || apply.getDeleted() == 1) throw new BusinessException(404, "申请不存在");
        if (!apply.getTargetUserId().equals(currentUserId))  throw new BusinessException(403, "无权处理");
        if (apply.getApplyStatus() != 0) throw new BusinessException(400, "该申请已处理");

        apply.setHandleTime(LocalDateTime.now());

        if (action == 1) {
            // ─── 同意 ───────────────────────────────────
            apply.setApplyStatus(1);
            applyMapper.updateById(apply);

            // 1. MySQL 存双向确认关系
            createBidirectionalRelation(apply);

            // 2. Nebula 写边（不影响主流程，异常只记日志）
            syncToNebula(apply);

            // 3. 异步触发推断（优先 Nebula，fallback Java）
            triggerInference(apply.getApplicantUserId(), apply.getTargetUserId());

        } else {
            // ─── 拒绝 ───────────────────────────────────
            apply.setApplyStatus(2);
            apply.setRejectReason(rejectReason);
            applyMapper.updateById(apply);
        }
    }

    /** 同步关系边到 Nebula */
    private void syncToNebula(UserRelationApply apply) {
        if (!nebulaUtil.isAvailable()) return;
        try {
            String chainJson = extractChainFromApply(apply);
            if (chainJson == null) return;
            List<String> chain = inferUtil.jsonToChain(chainJson);
            Integer gA = getGender(apply.getApplicantUserId());
            Integer gB = getGender(apply.getTargetUserId());
            nebulaUtil.syncRelationToGraph(chain, apply.getApplicantUserId(),
                    apply.getTargetUserId(), gA, gB);
        } catch (Exception e) {
            log.error("Nebula 同步关系失败（不影响主流程）: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    // 推断核心（Nebula 图遍历 + Java fallback）
    // ═══════════════════════════════════════════════════

    @Async
    public void triggerInference(Long userA, Long userB) {
        if (nebulaUtil.isAvailable()) {
            propagateNewEdge(userA, userB);
        } else {
            triggerInferenceFallback(userA, userB);
        }
    }

    /**
     * 新增边 A-B 后，全网络级推断扩散
     * 1. A视角：遍历B邻居，推断A与各人关系
     * 2. B视角：遍历A邻居，推断B与各人关系
     * 3. B的已有亲属C视角：推断C与A网络的关系（B-C已存在时A-B新建的扩散）
     * 4. A的已有亲属D视角：推断D与B网络的关系
     */
    private void propagateNewEdge(Long userA, Long userB) {
        try {
            // 双向推断
            triggerInferenceByGraph(userA, userB);
            triggerInferenceByGraph(userB, userA);

            // B 的 2 跳邻居对 A 网络推断
            Set<Long> bNeighbors = nebulaUtil.findNeighbors(userB, 2);
            bNeighbors.remove(userA);
            bNeighbors.remove(userB);
            for (Long c : bNeighbors) {
                triggerInferenceByGraph(c, userA);
                triggerInferenceByGraph(userA, c);
            }

            // A 的 2 跳邻居对 B 网络推断
            Set<Long> aNeighbors = nebulaUtil.findNeighbors(userA, 2);
            aNeighbors.remove(userA);
            aNeighbors.remove(userB);
            for (Long d : aNeighbors) {
                triggerInferenceByGraph(d, userB);
                triggerInferenceByGraph(userB, d);
            }

        } catch (Exception e) {
            log.error("[推断扩散] 异常: A={}, B={}", userA, userB, e);
        }
    }

    /**
     * 【主逻辑】用 Nebula 图遍历推断 userA 与其他人的关系
     * 1. 以新确认的 userB 为起点，在 Nebula 中找 4 跳以内的所有邻居
     * 2. 排除已有直接关系的节点
     * 3. 对每个候选节点，FIND ALL PATH，用 NebulaRelationResolver 解析称谓
     * 4. 将推断结果写入 MySQL（infer_status=1 待用户确认）
     */
    private void triggerInferenceByGraph(Long userA, Long userB) {
        try {
            log.info("[Nebula推断] 开始: userA={}, userB={}", userA, userB);

            // 以 userB 为起点找 4 跳邻居（包含 userA 的亲属网络）
            Set<Long> candidates = nebulaUtil.findNeighbors(userB, 4);
            candidates.remove(userA);
            candidates.remove(userB);
            if (candidates.isEmpty()) {
                log.info("[Nebula推断] 无候选节点，结束");
                return;
            }

            // 获取所有候选节点的性别（批量查）
            Set<Long> allIds = new HashSet<>(candidates);
            allIds.add(userA);
            Map<Long, Integer> genderMap = batchGetGender(allIds);

            int saved = 0;
            for (Long candidateId : candidates) {
                // 已有关系（手动或已确认推断）则跳过
                if (hasRelation(userA, candidateId)) continue;

                // 用 Nebula 找 userA → candidate 的所有路径（最多 4 跳）
                List<PathWrapper> paths = nebulaUtil.findPaths(userA, candidateId, 4);
                if (paths.isEmpty()) continue;

                // 解析为最佳称谓
                String kinship = nebulaResolver.resolveBest(paths, userA, genderMap);
                if (kinship == null || "亲属".equals(kinship)) continue;

                // 计算逆向称谓（candidate 称呼 userA）
                List<PathWrapper> reversePaths = nebulaUtil.findPaths(candidateId, userA, 4);
                String reverseKinship = nebulaResolver.resolveBest(reversePaths, candidateId, genderMap);
                if (reverseKinship == null) reverseKinship = "亲属";

                // 写入 MySQL（双向）
                boolean ok1 = saveInferredRelation(userA, candidateId, kinship);
                boolean ok2 = saveInferredRelation(candidateId, userA, reverseKinship);
                if (ok1 || ok2) saved++;
            }
            log.info("[Nebula推断] 完成: userA={}, 共推断 {} 组新关系", userA, saved);

        } catch (Exception e) {
            log.error("[Nebula推断] 异常: userA={}, userB={}", userA, userB, e);
            // 降级到 Java 推断
            triggerInferenceFallback(userA, userB);
        }
    }

    /**
     * 【Fallback】原 Java 链式推断逻辑（Nebula 不可用时使用）
     */
    private void triggerInferenceFallback(Long userA, Long userB) {
        try {
            log.info("[Java推断-Fallback] 开始: userA={}, userB={}", userA, userB);
            UserRelation a2bRel = getDirectRelation(userA, userB);
            if (a2bRel == null) return;
            UserRelation b2aRel = getDirectRelation(userB, userA);
            List<String> chainA2B = inferUtil.jsonToChain(a2bRel.getRelationChain());
            List<String> chainB2A = b2aRel != null
                    ? inferUtil.jsonToChain(b2aRel.getRelationChain()) : new ArrayList<>();

            // A 视角：遍历 B 的关系推断 A-C
            for (UserRelation bRel : getConfirmedRelations(userB)) {
                Long c = bRel.getRelatedUserId();
                if (c.equals(userA)) continue;
                List<String> chainB2C = inferUtil.jsonToChain(bRel.getRelationChain());
                List<String> inferred = inferUtil.inferChain(chainA2B, chainB2C);
                if (inferred != null && !inferred.isEmpty()) {
                    Integer genderC = getGender(c);
                    List<String> corrected = correctSiblingGender(inferred, genderC);
                    List<String> reversed  = inferUtil.reverseChainWithGender(
                            corrected, getGender(userA), genderC);
                    if (!hasRelation(userA, c)) {
                        saveInferredRelation(userA, c, inferUtil.resolveChain(corrected));
                        saveInferredRelation(c, userA, inferUtil.resolveChain(reversed));
                    }
                }
            }
            // B 视角：遍历 A 的关系推断 B-X
            if (!chainB2A.isEmpty()) {
                for (UserRelation aRel : getConfirmedRelations(userA)) {
                    Long x = aRel.getRelatedUserId();
                    if (x.equals(userB)) continue;
                    List<String> chainA2X = inferUtil.jsonToChain(aRel.getRelationChain());
                    List<String> inferred = inferUtil.inferChain(chainB2A, chainA2X);
                    if (inferred != null && !inferred.isEmpty()) {
                        Integer genderX = getGender(x);
                        List<String> corrected = correctSiblingGender(inferred, genderX);
                        List<String> reversed  = inferUtil.reverseChainWithGender(
                                corrected, getGender(userB), genderX);
                        if (!hasRelation(userB, x)) {
                            saveInferredRelation(userB, x, inferUtil.resolveChain(corrected));
                            saveInferredRelation(x, userB, inferUtil.resolveChain(reversed));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Java推断-Fallback] 异常: userA={}, userB={}", userA, userB, e);
        }
    }

    // ═══════════════════════════════════════════════════
    // 确认 / 拒绝推断关系
    // ═══════════════════════════════════════════════════

    public void confirmInferred(Long userId, Long relationId) {
        UserRelation rel = relationMapper.selectById(relationId);
        if (rel == null || !rel.getUserId().equals(userId)) throw new BusinessException(403, "无权操作");
        rel.setInferStatus(2);
        rel.setConfirmStatus(1);
        rel.setConfirmTime(LocalDateTime.now());
        relationMapper.updateById(rel);
        // 确认后同步边到 Nebula，并继续扩散推断
        if (nebulaUtil.isAvailable()) {
            List<String> chain = inferUtil.jsonToChain(rel.getRelationChain());
            nebulaUtil.syncRelationToGraph(chain, userId, rel.getRelatedUserId(),
                    getGender(userId), getGender(rel.getRelatedUserId()));
        }
        triggerInference(userId, rel.getRelatedUserId());
    }

    public void rejectInferred(Long userId, Long relationId) {
        UserRelation rel = relationMapper.selectById(relationId);
        if (rel == null || !rel.getUserId().equals(userId)) throw new BusinessException(403, "无权操作");
        rel.setDeleted(1);
        relationMapper.updateById(rel);
    }

    // ═══════════════════════════════════════════════════
    // 删除关系
    // ═══════════════════════════════════════════════════

    @Transactional(rollbackFor = Exception.class)
    public void removeRelation(Long userId, Long relationId) {
        UserRelation rel = relationMapper.selectById(relationId);
        if (rel == null || !rel.getUserId().equals(userId)) throw new BusinessException(403, "无权操作");
        // 删 MySQL（双向）
        LambdaQueryWrapper<UserRelation> w = new LambdaQueryWrapper<>();
        w.and(q -> q
            .eq(UserRelation::getUserId, userId)
            .eq(UserRelation::getRelatedUserId, rel.getRelatedUserId())
            .or()
            .eq(UserRelation::getUserId, rel.getRelatedUserId())
            .eq(UserRelation::getRelatedUserId, userId));
        relationMapper.delete(w);
        // 删 Nebula 边
        nebulaUtil.removeRelationFromGraph(userId, rel.getRelatedUserId());
    }

    // ═══════════════════════════════════════════════════
    // 查询
    // ═══════════════════════════════════════════════════

    public List<Map<String, Object>> getMyRelations(Long userId) {
        LambdaQueryWrapper<UserRelation> w = new LambdaQueryWrapper<>();
        w.eq(UserRelation::getUserId, userId)
         .eq(UserRelation::getConfirmStatus, 1)
         .in(UserRelation::getInferStatus, 0, 2)
         .eq(UserRelation::getDeleted, 0);
        return buildRelationList(w);
    }

    public List<Map<String, Object>> getMyPendingInferred(Long userId) {
        LambdaQueryWrapper<UserRelation> w = new LambdaQueryWrapper<>();
        w.eq(UserRelation::getUserId, userId)
         .eq(UserRelation::getInferStatus, 1)
         .eq(UserRelation::getConfirmStatus, 0)
         .eq(UserRelation::getDeleted, 0);
        return buildRelationList(w);
    }

    public List<Map<String, Object>> getPendingApplies(Long userId) {
        LambdaQueryWrapper<UserRelationApply> w = new LambdaQueryWrapper<>();
        w.eq(UserRelationApply::getTargetUserId, userId)
         .eq(UserRelationApply::getApplyStatus, 0)
         .eq(UserRelationApply::getDeleted, 0)
         .orderByDesc(UserRelationApply::getCreateTime);
        List<UserRelationApply> applies = applyMapper.selectList(w);
        if (applies.isEmpty()) return new ArrayList<>();
        List<Long> ids = applies.stream().map(UserRelationApply::getApplicantUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return applies.stream().map(a -> {
            Map<String, Object> item = new HashMap<>();
            item.put("applyId", a.getId());
            item.put("applicantUserId", a.getApplicantUserId());
            item.put("relationType", a.getRelationType());
            item.put("relationDesc", a.getRelationDesc());
            String cleanReason = a.getReason() != null && a.getReason().startsWith("[CHAIN]")
                    ? a.getReason().replaceFirst("\\[CHAIN]\\S*\\s?", "") : a.getReason();
            item.put("reason", cleanReason);
            item.put("createTime", a.getCreateTime());
            User u = userMap.get(a.getApplicantUserId());
            if (u != null) {
                item.put("applicantName", u.getRealName() != null ? u.getRealName() : "用户" + u.getId());
                item.put("applicantPhone", maskPhone(u.getPhone()));
            }
            return item;
        }).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════
    // 私有辅助方法
    // ═══════════════════════════════════════════════════

    private void createBidirectionalRelation(UserRelationApply apply) {
        String chainJson = extractChainFromApply(apply);
        List<String> chain = chainJson != null ? inferUtil.jsonToChain(chainJson) : new ArrayList<>();
        Integer gA = getGender(apply.getApplicantUserId());
        Integer gB = getGender(apply.getTargetUserId());

        List<String> correctedChain   = correctChainGender(chain, gA);
        List<String> reverseChain     = inferUtil.reverseChainWithGender(correctedChain, gA, gB);
        String forwardName  = resolveChainWithGender(correctedChain, gA);
        String reverseName  = resolveChainWithGender(reverseChain, gB);

        UserRelation r1 = buildRelation(
            apply.getApplicantUserId(), apply.getTargetUserId(),
            apply.getRelationType(), forwardName,
            inferUtil.chainToJson(correctedChain), 0, 1
        );
        UserRelation r2 = buildRelation(
            apply.getTargetUserId(), apply.getApplicantUserId(),
            apply.getRelationType(), reverseName,
            inferUtil.chainToJson(reverseChain), 0, 1
        );
        relationMapper.insert(r1);
        relationMapper.insert(r2);
    }

    private UserRelation buildRelation(Long userId, Long relatedUserId, Integer relType,
                                        String desc, String chainJson, int inferStatus, int confirmStatus) {
        UserRelation r = new UserRelation();
        r.setUserId(userId);
        r.setRelatedUserId(relatedUserId);
        r.setRelationType(relType);
        r.setRelationDesc(desc);
        r.setRelationChain(chainJson);
        r.setInferStatus(inferStatus);
        r.setConfirmStatus(confirmStatus);
        if (confirmStatus == 1) r.setConfirmTime(LocalDateTime.now());
        return r;
    }

    /** 保存推断关系，返回是否新建成功（已存在返回 false）*/
    private boolean saveInferredRelation(Long fromUser, Long toUser, String kinship) {
        if (kinship == null || "亲属".equals(kinship)) return false;
        LambdaQueryWrapper<UserRelation> check = new LambdaQueryWrapper<>();
        check.eq(UserRelation::getUserId, fromUser)
             .eq(UserRelation::getRelatedUserId, toUser)
             .eq(UserRelation::getDeleted, 0);
        if (relationMapper.selectCount(check) > 0) return false;

        UserRelation rel = new UserRelation();
        rel.setUserId(fromUser);
        rel.setRelatedUserId(toUser);
        rel.setRelationType(99);
        rel.setRelationDesc(kinship);
        rel.setRelationChain("[]");
        rel.setInferStatus(2);           // 2=推断已自动确认（不再需要手动点确认）
        rel.setConfirmStatus(1);         // 直接进入已确认列表，各用户均可见
        rel.setConfirmTime(LocalDateTime.now());
        relationMapper.insert(rel);
        log.info("[推断] 新增已自动确认关系: from={}, to={}, kinship={}", fromUser, toUser, kinship);
        return true;
    }

    private boolean hasRelation(Long userId, Long relatedUserId) {
        LambdaQueryWrapper<UserRelation> w = new LambdaQueryWrapper<>();
        w.eq(UserRelation::getUserId, userId)
         .eq(UserRelation::getRelatedUserId, relatedUserId)
         .eq(UserRelation::getDeleted, 0);
        return relationMapper.selectCount(w) > 0;
    }

    private List<UserRelation> getConfirmedRelations(Long userId) {
        LambdaQueryWrapper<UserRelation> w = new LambdaQueryWrapper<>();
        w.eq(UserRelation::getUserId, userId)
         .eq(UserRelation::getConfirmStatus, 1)
         .in(UserRelation::getInferStatus, 0, 2)
         .eq(UserRelation::getDeleted, 0);
        return relationMapper.selectList(w);
    }

    private UserRelation getDirectRelation(Long fromUser, Long toUser) {
        LambdaQueryWrapper<UserRelation> w = new LambdaQueryWrapper<>();
        w.eq(UserRelation::getUserId, fromUser)
         .eq(UserRelation::getRelatedUserId, toUser)
         .eq(UserRelation::getConfirmStatus, 1)
         .eq(UserRelation::getDeleted, 0)
         .last("LIMIT 1");
        return relationMapper.selectOne(w);
    }

    private String getRelationStatus(Long userId1, Long userId2) {
        LambdaQueryWrapper<UserRelation> rw = new LambdaQueryWrapper<>();
        rw.and(w -> w.eq(UserRelation::getUserId, userId1).eq(UserRelation::getRelatedUserId, userId2)
                .or().eq(UserRelation::getUserId, userId2).eq(UserRelation::getRelatedUserId, userId1))
          .eq(UserRelation::getConfirmStatus, 1).eq(UserRelation::getDeleted, 0);
        if (relationMapper.selectCount(rw) > 0) return "related";
        LambdaQueryWrapper<UserRelationApply> aw = new LambdaQueryWrapper<>();
        aw.and(w -> w.eq(UserRelationApply::getApplicantUserId, userId1).eq(UserRelationApply::getTargetUserId, userId2)
                .or().eq(UserRelationApply::getApplicantUserId, userId2).eq(UserRelationApply::getTargetUserId, userId1))
          .eq(UserRelationApply::getApplyStatus, 0).eq(UserRelationApply::getDeleted, 0);
        if (applyMapper.selectCount(aw) > 0) return "pending";
        return "none";
    }

    // ── 性别相关 ──────────────────────────────────────

    private Integer getGender(Long userId) {
        LambdaQueryWrapper<UserProfile> w = new LambdaQueryWrapper<>();
        w.eq(UserProfile::getUserId, userId).last("LIMIT 1");
        UserProfile p = userProfileMapper.selectOne(w);
        return p != null ? p.getGender() : null;
    }

    private Map<Long, Integer> batchGetGender(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return new HashMap<>();
        LambdaQueryWrapper<UserProfile> w = new LambdaQueryWrapper<>();
        w.in(UserProfile::getUserId, userIds);
        Map<Long, Integer> result = new HashMap<>();
        for (UserProfile p : userProfileMapper.selectList(w)) {
            result.put(p.getUserId(), p.getGender() != null ? p.getGender() : 0);
        }
        return result;
    }

    // ── 链辅助 ───────────────────────────────────────

    private String extractChainFromApply(UserRelationApply apply) {
        String reason = apply.getReason();
        if (reason != null && reason.startsWith("[CHAIN]")) {
            int end = reason.indexOf(' ', 7);
            return end > 0 ? reason.substring(7, end) : reason.substring(7);
        }
        return null;
    }

    private List<String> correctChainGender(List<String> chain, Integer myGender) {
        return chain; // 正向链由前端确定，无需修正
    }

    private String resolveChainWithGender(List<String> chain, Integer myGender) {
        if (chain == null || chain.isEmpty()) return "亲属";
        return inferUtil.resolveChain(chain);
    }

    private List<String> correctSiblingGender(List<String> chain, Integer targetGender) {
        if (chain == null || chain.size() < 3 || !"同辈".equals(chain.get(0))) return chain;
        List<String> copy = new ArrayList<>(chain);
        if (targetGender != null && targetGender == 2) {
            String sib = copy.get(2);
            if ("哥".equals(sib) || "弟".equals(sib)) copy.set(2, "妹");
            else if ("姐".equals(sib)) copy.set(2, "妹");
        } else if (targetGender != null && targetGender == 1) {
            String sib = copy.get(2);
            if ("姐".equals(sib) || "妹".equals(sib)) copy.set(2, "弟");
            else if ("哥".equals(sib)) copy.set(2, "弟");
        }
        return copy;
    }

    private Integer chainToType(List<String> chain) {
        if (chain == null || chain.isEmpty()) return 99;
        if ("配偶".equals(chain.get(0))) return 3;
        if ("同辈".equals(chain.get(0))) return 5;
        String first = chain.get(0);
        if ("父".equals(first)) return 1;
        if ("母".equals(first)) return 2;
        if ("子".equals(first) || "女".equals(first)) return 4;
        return 99;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    // ── 列表构建 ─────────────────────────────────────

    private List<Map<String, Object>> buildRelationList(LambdaQueryWrapper<UserRelation> w) {
        List<UserRelation> rels = relationMapper.selectList(w);
        if (rels.isEmpty()) return new ArrayList<>();
        List<Long> ids = rels.stream().map(UserRelation::getRelatedUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return rels.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("relationId", r.getId());
            item.put("relatedUserId", r.getRelatedUserId());
            item.put("relationType", r.getRelationType());
            item.put("relationDesc", r.getRelationDesc());
            item.put("relationChain", r.getRelationChain());
            item.put("inferStatus", r.getInferStatus());
            item.put("confirmTime", r.getConfirmTime());
            User u = userMap.get(r.getRelatedUserId());
            if (u != null) {
                item.put("realName", u.getRealName() != null ? u.getRealName() : "用户" + u.getId());
                item.put("phone", maskPhone(u.getPhone()));
                item.put("lifeStatus", u.getLifeStatus());
                item.put("realNameStatus", u.getRealNameStatus());
            }
            return item;
        }).collect(Collectors.toList());
    }
}
