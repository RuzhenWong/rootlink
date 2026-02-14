package com.rootlink.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rootlink.backend.entity.*;
import com.rootlink.backend.exception.BusinessException;
import com.rootlink.backend.mapper.*;
import com.rootlink.backend.utils.NebulaUtil;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 挽联缅怀核心服务
 * 
 * 核心功能：
 * 1. 直系两代圈定算法（NebulaGraph查询）
 * 2. 全员100%通过审核机制
 * 3. 一票否决制
 * 4. 无直系亲属禁用挽联
 */
@Slf4j
@Service
public class EulogyService {

    @Autowired
    private EulogyMapper eulogyMapper;

    @Autowired
    private EulogyReviewerMapper reviewerMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRelationMapper relationMapper;

    @Autowired(required = false)
    private NebulaUtil nebulaUtil;

    /**
     * 提交挽联
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitEulogy(Long submitterUserId, Long targetUserId, String content) {
        // 1. 验证目标用户必须是已离世状态
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(404, "目标用户不存在");
        }
        if (targetUser.getLifeStatus() != 3) {
            throw new BusinessException(400, "只能为已离世的用户提交挽联");
        }

        // 2. 获取直系两代亲属圈（核心算法）
        List<DirectRelative> directRelatives = getDirectTwoGenerationRelatives(targetUserId);

        // 3. 验证是否有直系亲属（无直系亲属禁用挽联功能）
        if (directRelatives.isEmpty()) {
            throw new BusinessException(400, "该用户无直系亲属，无法提交挽联");
        }

        // 4. 创建挽联记录
        Eulogy eulogy = new Eulogy();
        eulogy.setTargetUserId(targetUserId);
        eulogy.setSubmitterUserId(submitterUserId);
        eulogy.setContent(content);
        eulogy.setSubmitTime(LocalDateTime.now());
        eulogy.setReviewStatus(0);  // 待审核
        eulogyMapper.insert(eulogy);

        // 5. 为每个直系两代亲属创建审核记录
        for (DirectRelative relative : directRelatives) {
            EulogyReviewer reviewer = new EulogyReviewer();
            reviewer.setEulogyId(eulogy.getId());
            reviewer.setReviewerUserId(relative.getUserId());
            reviewer.setRelationType(relative.getRelationType());
            reviewer.setReviewStatus(0);  // 待审核
            reviewerMapper.insert(reviewer);
        }

        log.info("挽联已提交: eulogyId={}, targetUserId={}, reviewerCount={}", 
            eulogy.getId(), targetUserId, directRelatives.size());
    }

    /**
     * 审核挽联（全员100%通过机制 + 一票否决）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewEulogy(Long reviewerUserId, Long eulogyId, Integer reviewStatus, String reviewOpinion) {
        // 1. 查询挽联
        Eulogy eulogy = eulogyMapper.selectById(eulogyId);
        if (eulogy == null) {
            throw new BusinessException(404, "挽联不存在");
        }

        if (eulogy.getReviewStatus() != 0) {
            throw new BusinessException(400, "该挽联已完成审核");
        }

        // 2. 查询审核人记录
        LambdaQueryWrapper<EulogyReviewer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EulogyReviewer::getEulogyId, eulogyId);
        wrapper.eq(EulogyReviewer::getReviewerUserId, reviewerUserId);
        EulogyReviewer reviewer = reviewerMapper.selectOne(wrapper);

        if (reviewer == null) {
            throw new BusinessException(403, "您不是该挽联的审核人");
        }

        if (reviewer.getReviewStatus() != 0) {
            throw new BusinessException(400, "您已审核过该挽联");
        }

        // 3. 更新审核人状态
        reviewer.setReviewStatus(reviewStatus);
        reviewer.setReviewTime(LocalDateTime.now());
        reviewer.setReviewOpinion(reviewOpinion);
        reviewerMapper.updateById(reviewer);

        log.info("挽联审核已提交: eulogyId={}, reviewerUserId={}, status={}", 
            eulogyId, reviewerUserId, reviewStatus);

        // 4. 一票否决制：任何一人拒绝立即拒绝整个挽联
        if (reviewStatus == 2) {
            eulogy.setReviewStatus(2);  // 审核拒绝
            eulogy.setRejectReason(reviewOpinion);
            eulogy.setRejectorUserId(reviewerUserId);
            eulogy.setRejectTime(LocalDateTime.now());
            eulogyMapper.updateById(eulogy);

            log.info("挽联被拒绝（一票否决）: eulogyId={}, rejectorUserId={}", eulogyId, reviewerUserId);
            return;
        }

        // 5. 检查是否全员通过（100%通过机制）
        checkAllReviewersApproval(eulogyId);
    }

    /**
     * 检查是否全员通过
     */
    private void checkAllReviewersApproval(Long eulogyId) {
        LambdaQueryWrapper<EulogyReviewer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EulogyReviewer::getEulogyId, eulogyId);
        List<EulogyReviewer> reviewers = reviewerMapper.selectList(wrapper);

        // 统计审核状态
        long totalCount = reviewers.size();
        long approvedCount = reviewers.stream().filter(r -> r.getReviewStatus() == 1).count();
        long rejectedCount = reviewers.stream().filter(r -> r.getReviewStatus() == 2).count();
        long pendingCount = reviewers.stream().filter(r -> r.getReviewStatus() == 0).count();

        log.info("挽联审核进度: eulogyId={}, total={}, approved={}, rejected={}, pending={}", 
            eulogyId, totalCount, approvedCount, rejectedCount, pendingCount);

        // 如果有拒绝，已经在上面处理了
        if (rejectedCount > 0) {
            return;
        }

        // 全员通过（100%）才发布
        if (approvedCount == totalCount && pendingCount == 0) {
            Eulogy eulogy = eulogyMapper.selectById(eulogyId);
            eulogy.setReviewStatus(1);  // 审核通过
            eulogy.setPublishTime(LocalDateTime.now());
            eulogyMapper.updateById(eulogy);

            log.info("挽联审核通过（全员100%通过）: eulogyId={}, reviewerCount={}", eulogyId, totalCount);
        }
    }

    /**
     * 获取直系两代亲属圈（核心算法）
     * 
     * 直系两代定义：
     * 1. 父母（第一代长辈）
     * 2. 配偶（同代）
     * 3. 子女（第一代晚辈）
     * 4. 祖父母（第二代长辈）
     * 5. 孙子女（第二代晚辈）
     */
    private List<DirectRelative> getDirectTwoGenerationRelatives(Long targetUserId) {
        List<DirectRelative> relatives = new ArrayList<>();

        try {
            if (nebulaUtil != null && nebulaUtil.isAvailable()) {
                // 方式1：使用NebulaGraph查询（推荐）
                relatives = queryDirectRelativesFromNebula(targetUserId);
            } else {
                // 方式2：从MySQL查询（fallback）
                log.warn("NebulaGraph未配置，使用MySQL查询直系亲属");
                relatives = queryDirectRelativesFromMySQL(targetUserId);
            }
        } catch (Exception e) {
            log.error("查询直系亲属失败，使用MySQL查询", e);
            relatives = queryDirectRelativesFromMySQL(targetUserId);
        }

        return relatives;
    }

    /**
     * 使用NebulaGraph查询直系两代亲属
     */
    private List<DirectRelative> queryDirectRelativesFromNebula(Long targetUserId) {
        List<DirectRelative> relatives = new ArrayList<>();

        try {
            // 1. 查询父母（第一代长辈）
            String parentsQuery = String.format(
                "MATCH (p:Person)-[:Parent_Child]->(target:Person) " +
                "WHERE id(target) == %d " +
                "RETURN id(p) AS user_id, 1 AS relation_type", 
                targetUserId
            );
            ResultSet parentResult = nebulaUtil.executeQuery(parentsQuery);
            relatives.addAll(parseNebulaResult(parentResult, 1));

            // 2. 查询配偶（同代）
            String spouseQuery = String.format(
                "MATCH (target:Person)-[:Spouse]-(s:Person) " +
                "WHERE id(target) == %d " +
                "RETURN id(s) AS user_id, 2 AS relation_type",
                targetUserId
            );
            ResultSet spouseResult = nebulaUtil.executeQuery(spouseQuery);
            relatives.addAll(parseNebulaResult(spouseResult, 2));

            // 3. 查询子女（第一代晚辈）
            String childrenQuery = String.format(
                "MATCH (target:Person)-[:Parent_Child]->(c:Person) " +
                "WHERE id(target) == %d " +
                "RETURN id(c) AS user_id, 3 AS relation_type",
                targetUserId
            );
            ResultSet childrenResult = nebulaUtil.executeQuery(childrenQuery);
            relatives.addAll(parseNebulaResult(childrenResult, 3));

            // 4. 查询祖父母（第二代长辈）
            String grandparentsQuery = String.format(
                "MATCH (gp:Person)-[:Parent_Child]->(p:Person)-[:Parent_Child]->(target:Person) " +
                "WHERE id(target) == %d " +
                "RETURN id(gp) AS user_id, 4 AS relation_type",
                targetUserId
            );
            ResultSet grandparentsResult = nebulaUtil.executeQuery(grandparentsQuery);
            relatives.addAll(parseNebulaResult(grandparentsResult, 4));

            // 5. 查询孙子女（第二代晚辈）
            String grandchildrenQuery = String.format(
                "MATCH (target:Person)-[:Parent_Child]->(c:Person)-[:Parent_Child]->(gc:Person) " +
                "WHERE id(target) == %d " +
                "RETURN id(gc) AS user_id, 5 AS relation_type",
                targetUserId
            );
            ResultSet grandchildrenResult = nebulaUtil.executeQuery(grandchildrenQuery);
            relatives.addAll(parseNebulaResult(grandchildrenResult, 5));

            log.info("NebulaGraph查询直系两代亲属: targetUserId={}, count={}", targetUserId, relatives.size());

        } catch (Exception e) {
            log.error("NebulaGraph查询失败", e);
            throw new RuntimeException("图数据库查询失败", e);
        }

        return relatives;
    }

    /**
     * 解析NebulaGraph查询结果
     */
    private List<DirectRelative> parseNebulaResult(ResultSet resultSet, Integer relationType) {
        List<DirectRelative> relatives = new ArrayList<>();

        try {
            if (resultSet == null || !resultSet.isSucceeded()) {
                return relatives;
            }

            // Nebula Java Client 3.x 不提供 Iterator，
            // 正确方式：rowsSize() + rowValues(index)
            for (int i = 0; i < resultSet.rowsSize(); i++) {
                ResultSet.Record record = resultSet.rowValues(i);
                ValueWrapper userIdWrapper = record.get("user_id");
                if (userIdWrapper != null && !userIdWrapper.isNull()) {
                    Long userId = userIdWrapper.asLong();
                    DirectRelative relative = new DirectRelative();
                    relative.setUserId(userId);
                    relative.setRelationType(relationType);
                    relatives.add(relative);
                }
            }
        } catch (Exception e) {
            log.error("解析NebulaGraph结果失败", e);
        }

        return relatives;
    }

    /**
     * 从MySQL查询直系两代亲属（fallback方案）
     */
    private List<DirectRelative> queryDirectRelativesFromMySQL(Long targetUserId) {
        Set<DirectRelative> relatives = new HashSet<>();

        // 1. 查询父母
        LambdaQueryWrapper<UserRelation> parentWrapper = new LambdaQueryWrapper<>();
        parentWrapper.eq(UserRelation::getRelatedUserId, targetUserId);
        parentWrapper.in(UserRelation::getRelationType, Arrays.asList(1, 2));  // 父亲、母亲
        parentWrapper.eq(UserRelation::getConfirmStatus, 1);
        List<UserRelation> parents = relationMapper.selectList(parentWrapper);
        parents.forEach(r -> relatives.add(new DirectRelative(r.getUserId(), 1)));

        // 2. 查询配偶
        LambdaQueryWrapper<UserRelation> spouseWrapper = new LambdaQueryWrapper<>();
        spouseWrapper.and(w -> w.eq(UserRelation::getUserId, targetUserId)
            .or().eq(UserRelation::getRelatedUserId, targetUserId));
        spouseWrapper.eq(UserRelation::getRelationType, 3);  // 配偶
        spouseWrapper.eq(UserRelation::getConfirmStatus, 1);
        List<UserRelation> spouses = relationMapper.selectList(spouseWrapper);
        spouses.forEach(r -> {
            Long spouseId = r.getUserId().equals(targetUserId) ? r.getRelatedUserId() : r.getUserId();
            relatives.add(new DirectRelative(spouseId, 2));
        });

        // 3. 查询子女
        LambdaQueryWrapper<UserRelation> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(UserRelation::getUserId, targetUserId);
        childWrapper.eq(UserRelation::getRelationType, 4);  // 子女
        childWrapper.eq(UserRelation::getConfirmStatus, 1);
        List<UserRelation> children = relationMapper.selectList(childWrapper);
        children.forEach(r -> relatives.add(new DirectRelative(r.getRelatedUserId(), 3)));

        // 4. 查询祖父母（父母的父母）
        for (UserRelation parent : parents) {
            LambdaQueryWrapper<UserRelation> grandparentWrapper = new LambdaQueryWrapper<>();
            grandparentWrapper.eq(UserRelation::getRelatedUserId, parent.getUserId());
            grandparentWrapper.in(UserRelation::getRelationType, Arrays.asList(1, 2));
            grandparentWrapper.eq(UserRelation::getConfirmStatus, 1);
            List<UserRelation> grandparents = relationMapper.selectList(grandparentWrapper);
            grandparents.forEach(r -> relatives.add(new DirectRelative(r.getUserId(), 4)));
        }

        // 5. 查询孙子女（子女的子女）
        for (UserRelation child : children) {
            LambdaQueryWrapper<UserRelation> grandchildWrapper = new LambdaQueryWrapper<>();
            grandchildWrapper.eq(UserRelation::getUserId, child.getRelatedUserId());
            grandchildWrapper.eq(UserRelation::getRelationType, 4);
            grandchildWrapper.eq(UserRelation::getConfirmStatus, 1);
            List<UserRelation> grandchildren = relationMapper.selectList(grandchildWrapper);
            grandchildren.forEach(r -> relatives.add(new DirectRelative(r.getRelatedUserId(), 5)));
        }

        log.info("MySQL查询直系两代亲属: targetUserId={}, count={}", targetUserId, relatives.size());

        return new ArrayList<>(relatives);
    }

    /**
     * 获取我需要审核的挽联列表
     */
    public List<Eulogy> getMyPendingReviews(Long reviewerUserId) {
        // 查询我是审核人的挽联
        LambdaQueryWrapper<EulogyReviewer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EulogyReviewer::getReviewerUserId, reviewerUserId);
        wrapper.eq(EulogyReviewer::getReviewStatus, 0);  // 待审核
        List<EulogyReviewer> reviewers = reviewerMapper.selectList(wrapper);

        if (reviewers.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> eulogyIds = reviewers.stream()
            .map(EulogyReviewer::getEulogyId)
            .collect(Collectors.toList());

        LambdaQueryWrapper<Eulogy> eulogyWrapper = new LambdaQueryWrapper<>();
        eulogyWrapper.in(Eulogy::getId, eulogyIds);
        eulogyWrapper.eq(Eulogy::getReviewStatus, 0);  // 待审核状态
        eulogyWrapper.orderByDesc(Eulogy::getSubmitTime);

        return eulogyMapper.selectList(eulogyWrapper);
    }

    /**
     * 获取挽联墙（已通过审核的挽联）
     * targetUserId 为 null 时返回全部已发布挽联
     */
    public Page<Eulogy> getEulogyWall(Long targetUserId, Integer pageNum, Integer pageSize) {
        Page<Eulogy> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Eulogy> wrapper = new LambdaQueryWrapper<>();
        if (targetUserId != null) {
            wrapper.eq(Eulogy::getTargetUserId, targetUserId);
        }
        wrapper.eq(Eulogy::getReviewStatus, 1);  // 审核通过
        wrapper.orderByDesc(Eulogy::getPublishTime);

        return eulogyMapper.selectPage(page, wrapper);
    }

    /**
     * 获取挽联详情（包含审核人列表）
     */
    public Map<String, Object> getEulogyDetail(Long eulogyId) {
        Eulogy eulogy = eulogyMapper.selectById(eulogyId);
        if (eulogy == null) {
            throw new BusinessException(404, "挽联不存在");
        }

        LambdaQueryWrapper<EulogyReviewer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EulogyReviewer::getEulogyId, eulogyId);
        List<EulogyReviewer> reviewers = reviewerMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("eulogy", eulogy);
        result.put("reviewers", reviewers);

        return result;
    }

    /**
     * 直系亲属数据结构
     */
    private static class DirectRelative {
        private Long userId;
        private Integer relationType;  // 1-父母, 2-配偶, 3-子女, 4-祖父母, 5-孙子女

        public DirectRelative() {}

        public DirectRelative(Long userId, Integer relationType) {
            this.userId = userId;
            this.relationType = relationType;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Integer getRelationType() {
            return relationType;
        }

        public void setRelationType(Integer relationType) {
            this.relationType = relationType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DirectRelative that = (DirectRelative) o;
            return Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId);
        }
    }
}
