package com.rootlink.backend.common;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    // 通用错误码
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    // 用户相关错误码 (40001-40099)
    USER_NOT_FOUND(40001, "用户不存在"),
    PASSWORD_ERROR(40002, "密码错误"),
    PHONE_EXISTS(40003, "手机号已存在"),
    VERIFICATION_CODE_ERROR(40004, "验证码错误"),
    ACCOUNT_DISABLED(40005, "账号已禁用"),
    ACCOUNT_DECEASED(40006, "该账号用户已离世"),
    
    // 实名认证相关错误码 (40100-40199)
    MUST_REAL_NAME_FIRST(40100, "请先完成实名认证"),
    ID_CARD_ALREADY_USED(40101, "该身份证号已被使用"),
    REAL_NAME_VERIFY_FAILED(40102, "实名认证失败,请检查姓名和身份证号是否匹配"),
    ALREADY_REAL_NAME(40103, "您已完成实名认证"),
    
    // Token相关错误码 (40200-40299)
    TOKEN_EXPIRED(40200, "登录已过期,请重新登录"),
    TOKEN_INVALID(40201, "无效的Token"),
    
    // 验证码相关错误码 (40300-40399)
    SMS_SEND_FAILED(40300, "短信发送失败"),
    SMS_SEND_TOO_FREQUENT(40301, "发送过于频繁,请稍后再试"),
    ;

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
