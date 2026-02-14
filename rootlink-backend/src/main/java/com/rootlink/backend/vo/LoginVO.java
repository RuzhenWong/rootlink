package com.rootlink.backend.vo;

import lombok.Data;

/**
 * 登录响应VO
 */
@Data
public class LoginVO {

    /**
     * Token
     */
    private String token;

    /**
     * RefreshToken
     */
    private String refreshToken;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 实名状态
     */
    private Integer realNameStatus;

    /**
     * Token过期时间(毫秒时间戳)
     */
    private Long expireTime;
}
