package com.rootlink.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户UUID(业务唯一标识)
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
     * 密码(BCrypt加密)
     */
    private String password;

    /**
     * 密码盐值
     */
    private String salt;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 身份证号(AES-256加密)
     */
    private String idCardEncrypted;

    /**
     * 身份证号SHA-256哈希(用于唯一性校验)
     */
    private String idCardHash;

    /**
     * 实名状态: 0-未实名, 1-审核中, 2-已实名, 3-审核失败
     */
    private Integer realNameStatus;

    /**
     * 实名认证通过时间
     */
    private LocalDateTime realNameTime;

    /**
     * 账号状态: 0-禁用, 1-正常, 2-冻结, 3-已注销
     */
    private Integer status;

    /**
     * 生命状态: 0-活跃, 1-不活跃, 2-疑似离世, 3-已离世
     */
    private Integer lifeStatus;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 是否允许被搜索: 0-否, 1-是
     */
    private Boolean allowSearch;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除: 0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
