package com.rootlink.backend.controller;

import com.rootlink.backend.common.Result;
import com.rootlink.backend.dto.RealNameDTO;
import com.rootlink.backend.service.UserService;
import com.rootlink.backend.utils.CosUtil;
import com.rootlink.backend.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/user")
public class UserController {

    @Autowired private UserService userService;
    @Autowired private CosUtil cosUtil;

    @GetMapping("/current")
    public Result<UserInfoVO> getCurrentUserInfo(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getCurrentUserInfo(userId));
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> getFullProfile(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getFullProfile(userId));
    }

    /** 查看亲属详情（按隐私规则过滤） */
    @GetMapping("/relative/{relativeUserId}")
    public Result<Map<String, Object>> getRelativeProfile(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long relativeUserId) {
        return Result.success(userService.getRelativeProfile(userId, relativeUserId));
    }

    @PatchMapping("/settings")
    public Result<Void> updateBasicSettings(@RequestAttribute("userId") Long userId,
                                            @RequestBody Map<String, Object> body) {
        userService.updateBasicSettings(userId, body);
        return Result.success();
    }

    @PutMapping("/profile")
    public Result<Void> updateDetailProfile(@RequestAttribute("userId") Long userId,
                                             @RequestBody Map<String, Object> body) {
        userService.updateDetailProfile(userId, body);
        return Result.success();
    }

    /** 上传头像 → COS */
    @PostMapping("/avatar")
    public Result<Map<String, Object>> uploadAvatar(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) return Result.error(400, "文件为空");

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
            ? original.substring(original.lastIndexOf('.')).toLowerCase() : ".jpg";
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)"))
            return Result.error(400, "只支持图片格式（jpg/png/gif/webp）");
        if (file.getSize() > 5 * 1024 * 1024)
            return Result.error(400, "图片大小不能超过 5MB");

        try {
            // 删旧头像（可选，不影响主流程）
            Map<String, Object> old = userService.getFullProfile(userId);
            String oldUrl = (String) old.get("avatarUrl");
            if (oldUrl != null && oldUrl.contains("cos")) {
                cosUtil.deleteByUrl(oldUrl);
            }

            String url = cosUtil.uploadAvatar(userId, file);
            Map<String, Object> body = new HashMap<>();
            body.put("avatarUrl", url);
            userService.updateDetailProfile(userId, body);

            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            return Result.success(result);

        } catch (Exception e) {
            log.error("头像上传失败 userId={}", userId, e);
            return Result.error(500, "上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/realname/submit")
    public Result<Void> submitRealName(@RequestAttribute("userId") Long userId,
                                       @Valid @RequestBody RealNameDTO dto) {
        userService.submitRealName(userId, dto);
        return Result.success();
    }

    @GetMapping("/realname/status")
    public Result<Map<String, Object>> getRealNameStatus(@RequestAttribute("userId") Long userId) {
        return Result.success(userService.getRealNameStatus(userId));
    }
}
