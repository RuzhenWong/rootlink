package com.rootlink.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息VO
 */
@Data
public class UserInfoVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * UUID
     */
    private String uuid;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 实名状态
     */
    private Integer realNameStatus;

    /**
     * 账号状态
     */
    private Integer status;

    /**
     * 生命状态
     */
    private Integer lifeStatus;

    /**
     * 是否允许被搜索
     */
    private Boolean allowSearch;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
}
