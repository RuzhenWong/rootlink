package com.rootlink.backend.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 发送验证码DTO
 */
@Data
public class SendCodeDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 类型: 1-注册, 2-登录, 3-重置密码
     */
    @NotNull(message = "类型不能为空")
    private Integer type;
}
