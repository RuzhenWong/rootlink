package com.rootlink.backend.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rootlink.backend.common.ErrorCode;
import com.rootlink.backend.exception.BusinessException;
import com.rootlink.backend.dto.RealNameDTO;
import com.rootlink.backend.entity.*;
import com.rootlink.backend.mapper.*;
import com.rootlink.backend.utils.NebulaUtil;
import com.rootlink.backend.utils.SecurityUtil;
import com.rootlink.backend.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserService {

    @Autowired private UserMapper               userMapper;
    @Autowired private UserProfileMapper        userProfileMapper;
    @Autowired private UserRelationMapper       relationMapper;
    @Autowired private UserRelationApplyMapper  applyMapper;
    @Autowired private EulogyMapper             eulogyMapper;
    @Autowired private EulogyReviewerMapper     eulogyReviewerMapper;
    @Autowired private TestamentMapper          testamentMapper;
    @Autowired private TestamentReceiverMapper  testamentReceiverMapper;
    @Autowired private DeathMarkMapper          deathMarkMapper;
    @Autowired private DeathProofMapper         deathProofMapper;
    @Autowired private DeathStatusLogMapper     deathStatusLogMapper;
    @Autowired private SecurityUtil securityUtil;
    @Autowired private RealNameVerifyService realNameVerifyService;
    @Autowired(required = false) private NebulaUtil nebulaUtil;

    public UserInfoVO getCurrentUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(user, vo);
        vo.setUserId(user.getId());
        return vo;
    }

    public Map<String, Object> getFullProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        UserProfile profile = queryProfile(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("uuid", user.getUuid());
        result.put("phone", user.getPhone());
        result.put("email", user.getEmail());
        result.put("realName", user.getRealName());
        result.put("realNameStatus", user.getRealNameStatus());
        result.put("status", user.getStatus());
        result.put("lifeStatus", user.getLifeStatus());
        result.put("allowSearch", user.getAllowSearch());
        result.put("lastLoginTime", user.getLastLoginTime());
        result.put("lastLoginIp", user.getLastLoginIp());
        result.put("createTime", user.getCreateTime());
        if (profile != null) {
            result.put("gender", profile.getGender());
            result.put("nation", profile.getNation());
            result.put("birthDate", profile.getBirthDate());
            result.put("nativePlace", profile.getNativePlace());
            result.put("residence", profile.getResidence());
            result.put("wechat", profile.getWechat());
            result.put("qq", profile.getQq());
            result.put("workUnit", profile.getWorkUnit());
            result.put("position", profile.getPosition());
            result.put("education", profile.getEducation());
            result.put("maritalStatus", profile.getMaritalStatus());
            result.put("bio", profile.getBio());
            result.put("province", profile.getProvince());
            result.put("city", profile.getCity());
            result.put("district", profile.getDistrict());
            result.put("familyRank", profile.getFamilyRank());
            result.put("avatarUrl", profile.getAvatarUrl());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateBasicSettings(Long userId, Map<String, Object> body) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        // email 已移到 updateDetailProfile 处理
        if (body.containsKey("allowSearch")) user.setAllowSearch(Boolean.parseBoolean(body.get("allowSearch").toString()));
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDetailProfile(Long userId, Map<String, Object> body) {
        UserProfile profile = queryProfile(userId);
        boolean isNew = profile == null;
        if (isNew) { profile = new UserProfile(); profile.setUserId(userId); }
        if (body.containsKey("gender")) profile.setGender(intOrNull(body, "gender"));
        if (body.containsKey("nation")) profile.setNation(strOrNull(body, "nation"));
        if (body.containsKey("birthDate")) {
            Object bd = body.get("birthDate");
            profile.setBirthDate(bd != null && !bd.toString().isBlank() ? LocalDate.parse(bd.toString()) : null);
        }
        if (body.containsKey("nativePlace")) profile.setNativePlace(strOrNull(body, "nativePlace"));
        if (body.containsKey("residence")) profile.setResidence(strOrNull(body, "residence"));
        if (body.containsKey("wechat")) profile.setWechat(strOrNull(body, "wechat"));
        if (body.containsKey("qq")) profile.setQq(strOrNull(body, "qq"));
        if (body.containsKey("workUnit")) profile.setWorkUnit(strOrNull(body, "workUnit"));
        if (body.containsKey("position")) profile.setPosition(strOrNull(body, "position"));
        if (body.containsKey("education")) profile.setEducation(strOrNull(body, "education"));
        if (body.containsKey("maritalStatus")) profile.setMaritalStatus(intOrNull(body, "maritalStatus"));
        if (body.containsKey("bio")) profile.setBio(strOrNull(body, "bio"));
        // email 存在 user 表，但从 profile 接口统一更新
        if (body.containsKey("email")) {
            User u = userMapper.selectById(userId);
            if (u != null) { u.setEmail(strOrNull(body, "email")); userMapper.updateById(u); }
        }
        if (body.containsKey("province")) profile.setProvince(strOrNull(body, "province"));
        if (body.containsKey("city")) profile.setCity(strOrNull(body, "city"));
        if (body.containsKey("district")) profile.setDistrict(strOrNull(body, "district"));
        if (body.containsKey("familyRank")) profile.setFamilyRank(intOrNull(body, "familyRank"));
        if (body.containsKey("avatarUrl")) profile.setAvatarUrl(strOrNull(body, "avatarUrl"));
        if (isNew) userProfileMapper.insert(profile);
        else userProfileMapper.updateById(profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitRealName(Long userId, RealNameDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (user.getRealNameStatus() == 2) throw new BusinessException(ErrorCode.ALREADY_REAL_NAME);
        String idCardHash = securityUtil.sha256Hash(dto.getIdCard());
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.eq(User::getIdCardHash, idCardHash).ne(User::getId, userId);
        if (userMapper.selectCount(w) > 0) throw new BusinessException(ErrorCode.ID_CARD_ALREADY_USED);
        boolean verified = realNameVerifyService.verifyThreeElements(dto.getRealName(), dto.getIdCard(), user.getPhone());
        if (!verified) {
            user.setRealNameStatus(3);
            userMapper.updateById(user);
            throw new BusinessException(ErrorCode.REAL_NAME_VERIFY_FAILED);
        }
        user.setRealName(dto.getRealName());
        user.setIdCardEncrypted(securityUtil.aesEncrypt(dto.getIdCard()));
        user.setIdCardHash(idCardHash);
        user.setRealNameStatus(2);
        user.setRealNameTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("实名认证成功: userId={}", userId);
    }

    public Map<String, Object> getRealNameStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        Map<String, Object> r = new HashMap<>();
        r.put("realNameStatus", user.getRealNameStatus());
        r.put("realName", user.getRealName());
        r.put("realNameTime", user.getRealNameTime());
        return r;
    }

    // =============================================
    // 查看自己的身份证号（仅限本人，已实名才能调用）
    // =============================================

    public Map<String, Object> getMyIdCard(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (user.getRealNameStatus() != 2)
            throw new BusinessException(400, "您尚未完成实名认证");
        if (user.getIdCardEncrypted() == null || user.getIdCardEncrypted().isBlank())
            throw new BusinessException(400, "身份证信息不存在，请重新实名");
        String idCard = securityUtil.aesDecrypt(user.getIdCardEncrypted());
        Map<String, Object> r = new HashMap<>();
        r.put("realName", user.getRealName());
        r.put("idCard",   idCard);
        return r;
    }

    // =============================================
    // 注销账号（级联删除全部关联数据）
    // =============================================

    /**
     * 注销当前用户账号，按顺序级联删除所有关联数据：
     *  1. TestamentReceiver（硬删）
     *  2. Testament（软删）
     *  3. EulogyReviewer（硬删）
     *  4. Eulogy（软删）
     *  5. DeathStatusLog（硬删）
     *  6. DeathMark（软删）
     *  7. DeathProof（软删）
     *  8. UserRelationApply（软删）
     *  9. UserRelation（软删，双向）
     * 10. UserProfile（硬删）
     * 11. Nebula 图顶点及所有边
     * 12. User（软删，deleted=1）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deactivateAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1)
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        // ── 1. TestamentReceiver（硬删，无 deleted 字段）────────────
        List<Testament> myTestaments = testamentMapper.selectList(
                new LambdaQueryWrapper<Testament>().eq(Testament::getUserId, userId));
        if (!myTestaments.isEmpty()) {
            List<Long> tidList = myTestaments.stream().map(Testament::getId).toList();
            testamentReceiverMapper.delete(
                    new LambdaQueryWrapper<TestamentReceiver>()
                            .in(TestamentReceiver::getTestamentId, tidList));
        }
        // 被别人指定为接收者的记录
        testamentReceiverMapper.delete(
                new LambdaQueryWrapper<TestamentReceiver>()
                        .eq(TestamentReceiver::getReceiverUserId, userId));

        // ── 2. Testament（软删）────────────────────────────────────
        testamentMapper.delete(
                new LambdaQueryWrapper<Testament>().eq(Testament::getUserId, userId));

        // ── 3/4. EulogyReviewer + Eulogy ───────────────────────────
        List<Eulogy> relatedEulogies = eulogyMapper.selectList(
                new LambdaQueryWrapper<Eulogy>().and(w ->
                        w.eq(Eulogy::getTargetUserId, userId)
                         .or().eq(Eulogy::getSubmitterUserId, userId)));
        if (!relatedEulogies.isEmpty()) {
            List<Long> eidList = relatedEulogies.stream().map(Eulogy::getId).toList();
            eulogyReviewerMapper.delete(
                    new LambdaQueryWrapper<EulogyReviewer>()
                            .in(EulogyReviewer::getEulogyId, eidList));
        }
        // 作为审核人的记录
        eulogyReviewerMapper.delete(
                new LambdaQueryWrapper<EulogyReviewer>()
                        .eq(EulogyReviewer::getReviewerUserId, userId));
        eulogyMapper.delete(
                new LambdaQueryWrapper<Eulogy>().and(w ->
                        w.eq(Eulogy::getTargetUserId, userId)
                         .or().eq(Eulogy::getSubmitterUserId, userId)));

        // ── 5. DeathStatusLog（硬删，无 deleted 字段）──────────────
        deathStatusLogMapper.delete(
                new LambdaQueryWrapper<DeathStatusLog>().and(w ->
                        w.eq(DeathStatusLog::getUserId, userId)
                         .or().eq(DeathStatusLog::getOperatorId, userId)));

        // ── 6. DeathMark（软删）───────────────────────────────────
        deathMarkMapper.delete(
                new LambdaQueryWrapper<DeathMark>().and(w ->
                        w.eq(DeathMark::getTargetUserId, userId)
                         .or().eq(DeathMark::getMarkerUserId, userId)));

        // ── 7. DeathProof（软删）──────────────────────────────────
        deathProofMapper.delete(
                new LambdaQueryWrapper<DeathProof>().and(w ->
                        w.eq(DeathProof::getTargetUserId, userId)
                         .or().eq(DeathProof::getUploaderUserId, userId)));

        // ── 8. UserRelationApply（软删，双向）─────────────────────
        applyMapper.delete(
                new LambdaQueryWrapper<UserRelationApply>().and(w ->
                        w.eq(UserRelationApply::getApplicantUserId, userId)
                         .or().eq(UserRelationApply::getTargetUserId, userId)));

        // ── 9. UserRelation（软删，双向）──────────────────────────
        relationMapper.delete(
                new LambdaQueryWrapper<UserRelation>().and(w ->
                        w.eq(UserRelation::getUserId, userId)
                         .or().eq(UserRelation::getRelatedUserId, userId)));

        // ── 10. UserProfile（硬删，无 deleted 字段）────────────────
        userProfileMapper.delete(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));

        // ── 11. Nebula 图（删顶点及所有边）────────────────────────
        try {
            if (nebulaUtil != null && nebulaUtil.isAvailable()) {
                nebulaUtil.deletePersonVertex(userId);
            }
        } catch (Exception e) {
            log.warn("[注销] Nebula 删除失败 userId={}: {}", userId, e.getMessage());
        }

        // ── 12. User（软删）───────────────────────────────────────
        user.setDeleted(1);
        userMapper.updateById(user);
        log.info("[注销] userId={} realName={} 账号注销完成", userId, user.getRealName());
    }

    private UserProfile queryProfile(Long userId) {
        LambdaQueryWrapper<UserProfile> w = new LambdaQueryWrapper<>();
        w.eq(UserProfile::getUserId, userId);
        return userProfileMapper.selectOne(w);
    }

    private String strOrNull(Map<String, Object> b, String k) {
        Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? v.toString() : null;
    }
    private Integer intOrNull(Map<String, Object> b, String k) {
        Object v = b.get(k); return v != null ? Integer.valueOf(v.toString()) : null;
    }

    // ===== 查看亲属详情（根据隐私设置过滤） =====

    /**
     * currentUserId 查看 relativeUserId 的个人资料
     * 规则：亲属可以看基本信息，联系方式默认不公开（除非本人设置）
     */
    public Map<String, Object> getRelativeProfile(Long currentUserId, Long relativeUserId) {
        if (currentUserId.equals(relativeUserId)) return getFullProfile(currentUserId);

        User user = userMapper.selectById(relativeUserId);
        if (user == null || user.getDeleted() == 1)
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        UserProfile profile = queryProfile(relativeUserId);

        Map<String, Object> result = new HashMap<>();
        // 基本信息：均可见
        result.put("userId",         user.getId());
        result.put("realName",       user.getRealName() != null ? user.getRealName() : "用户" + user.getId());
        result.put("realNameStatus", user.getRealNameStatus());
        result.put("lifeStatus",     user.getLifeStatus());

        if (profile != null) {
            result.put("gender",        profile.getGender());
            result.put("birthDate",     profile.getBirthDate());
            result.put("deathDate",     profile.getDeathDate());
            result.put("nation",        profile.getNation());
            result.put("nativePlace",   profile.getNativePlace());
            result.put("province",      profile.getProvince());
            result.put("city",          profile.getCity());
            result.put("district",      profile.getDistrict());
            result.put("workUnit",      profile.getWorkUnit());
            result.put("position",      profile.getPosition());
            result.put("education",     profile.getEducation());
            result.put("maritalStatus", profile.getMaritalStatus());
            result.put("familyRank",    profile.getFamilyRank());
            result.put("bio",           profile.getBio());
            result.put("avatarUrl",     profile.getAvatarUrl());
            // 联系方式：需对方开启搜索才显示手机，其他联系方式开放
            if (Boolean.TRUE.equals(user.getAllowSearch())) {
                String phone = user.getPhone();
                result.put("phone",   phone != null && phone.length() >= 7
                    ? phone.substring(0,3) + "****" + phone.substring(7) : phone);
            }
            result.put("wechat", profile.getWechat());
            result.put("qq",     profile.getQq());
        }
        return result;
    }
}
