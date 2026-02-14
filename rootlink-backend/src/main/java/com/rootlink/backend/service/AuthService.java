package com.rootlink.backend.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rootlink.backend.common.ErrorCode;
import com.rootlink.backend.dto.LoginDTO;
import com.rootlink.backend.dto.RegisterDTO;
import com.rootlink.backend.entity.User;
import com.rootlink.backend.entity.UserProfile;
import com.rootlink.backend.exception.BusinessException;
import com.rootlink.backend.mapper.UserMapper;
import com.rootlink.backend.mapper.UserProfileMapper;
import com.rootlink.backend.utils.JwtUtil;
import com.rootlink.backend.utils.NebulaUtil;
import com.rootlink.backend.utils.RedisUtil;
import com.rootlink.backend.utils.SecurityUtil;
import com.rootlink.backend.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private SmsService smsService;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private NebulaUtil nebulaUtil;

    private static final String USER_TOKEN_PREFIX = "user:token:";

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 1. 校验验证码
        // TODO: 短信服务接入后，删除下方的 skipVerify 逻辑，恢复正式验证
        //       将 skipVerify 改为 false，或直接还原为：
        //       if (!smsService.verifyCode(dto.getPhone(), dto.getCode())) {
        //           throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        //       }
        boolean skipVerify = true;
        if (!skipVerify && !smsService.verifyCode(dto.getPhone(), dto.getCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }

        // 2. 检查手机号是否已注册
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        // 3. 创建用户
        User user = new User();
        user.setUuid(UUID.randomUUID().toString());
        user.setPhone(dto.getPhone());

        // 4. 密码加密
        String salt = securityUtil.generateSalt();
        String encryptedPassword = securityUtil.hashPassword(dto.getPassword(), salt);
        user.setPassword(encryptedPassword);
        user.setSalt(salt);

        user.setStatus(1); // 正常
        user.setLifeStatus(0); // 活跃
        user.setRealNameStatus(0); // 未实名
        user.setAllowSearch(true); // 默认允许被搜索

        userMapper.insert(user);

        // 同步到 NebulaGraph 顶点（不可用时跳过）
        nebulaUtil.insertPerson(user.getId(), user.getRealName(), null);

        // 5. 创建用户详情表
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        userProfileMapper.insert(profile);

        log.info("用户注册成功: phone={}, userId={}", dto.getPhone(), user.getId());
    }

    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO dto, String loginIp) {
        // 1. 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 校验密码
        boolean passwordMatch = securityUtil.verifyPassword(
                dto.getPassword(), user.getSalt(), user.getPassword()
        );
        if (!passwordMatch) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 3. 检查账号状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (user.getLifeStatus() == 3) {
            throw new BusinessException(ErrorCode.ACCOUNT_DECEASED);
        }

        // 4. 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("uuid", user.getUuid());
        claims.put("phone", user.getPhone());
        claims.put("realNameStatus", user.getRealNameStatus());

        String token = jwtUtil.generateToken(claims);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 5. 存储Token到Redis(用于单点登录控制)
        String tokenKey = USER_TOKEN_PREFIX + user.getId();
        redisUtil.set(tokenKey, token, 24, TimeUnit.HOURS);

        // 6. 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        userMapper.updateById(user);

        // 7. 构建返回数据
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setRefreshToken(refreshToken);
        vo.setUserId(user.getId());
        vo.setRealName(user.getRealName());
        vo.setRealNameStatus(user.getRealNameStatus());
        vo.setExpireTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000);

        log.info("用户登录成功: phone={}, userId={}", dto.getPhone(), user.getId());

        return vo;
    }

    /**
     * 登出
     */
    public void logout(Long userId) {
        String tokenKey = USER_TOKEN_PREFIX + userId;
        redisUtil.delete(tokenKey);
        log.info("用户登出: userId={}", userId);
    }
}
