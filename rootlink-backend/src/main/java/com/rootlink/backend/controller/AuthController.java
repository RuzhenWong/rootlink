package com.rootlink.backend.controller;

import com.rootlink.backend.common.Result;
import com.rootlink.backend.dto.LoginDTO;
import com.rootlink.backend.dto.RegisterDTO;
import com.rootlink.backend.dto.SendCodeDTO;
import com.rootlink.backend.service.AuthService;
import com.rootlink.backend.service.SmsService;
import com.rootlink.backend.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private SmsService smsService;

    /**
     * 发送短信验证码
     */
    @PostMapping("/sms/send")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        smsService.sendCode(dto.getPhone(), dto.getType());
        return Result.success();
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String loginIp = getClientIp(request);
        LoginVO vo = authService.login(dto, loginIp);
        return Result.success(vo);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute("userId") Long userId) {
        authService.logout(userId);
        return Result.success();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
