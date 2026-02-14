package com.rootlink.backend.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 用户注册DTO
 */
@Data
public class RegisterDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{6,20}$", 
             message = "密码需6-20位,包含大小写字母和数字")
    private String password;

    /**
     * 验证码
     * TODO: 短信服务接入后，恢复下方两个校验注解：
     *       @NotBlank(message = "验证码不能为空")
     *       @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
     */
    private String code;
}
