package com.rootlink.backend.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rootlink.backend.common.ErrorCode;
import com.rootlink.backend.dto.RealNameDTO;
import com.rootlink.backend.entity.User;
import com.rootlink.backend.entity.UserProfile;
import com.rootlink.backend.exception.BusinessException;
import com.rootlink.backend.mapper.UserMapper;
import com.rootlink.backend.mapper.UserProfileMapper;
import com.rootlink.backend.utils.SecurityUtil;
import com.rootlink.backend.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserService {

    @Autowired private UserMapper userMapper;
    @Autowired private UserProfileMapper userProfileMapper;
    @Autowired private SecurityUtil securityUtil;
    @Autowired private RealNameVerifyService realNameVerifyService;

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
            if (u != null) {
                u.setEmail(strOrNull(body, "email"));
                userMapper.updateById(u);
            }
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
