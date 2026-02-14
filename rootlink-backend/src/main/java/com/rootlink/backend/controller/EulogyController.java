package com.rootlink.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rootlink.backend.common.Result;
import com.rootlink.backend.entity.Eulogy;
import com.rootlink.backend.service.EulogyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/eulogy")
public class EulogyController {

    @Autowired
    private EulogyService eulogyService;

    /**
     * 提交挽联
     */
    @PostMapping("/submit")
    public Result<Void> submitEulogy(@RequestAttribute("userId") Long submitterUserId,
                                      @RequestBody Map<String, Object> params) {
        Long targetUserId = Long.valueOf(params.get("targetUserId").toString());
        String content = params.get("content").toString();

        eulogyService.submitEulogy(submitterUserId, targetUserId, content);
        return Result.success();
    }

    /**
     * 审核挽联
     */
    @PostMapping("/{eulogyId}/review")
    public Result<Void> reviewEulogy(@RequestAttribute("userId") Long reviewerUserId,
                                      @PathVariable Long eulogyId,
                                      @RequestBody Map<String, Object> params) {
        Integer reviewStatus = Integer.valueOf(params.get("reviewStatus").toString());
        String reviewOpinion = params.getOrDefault("reviewOpinion", "").toString();

        eulogyService.reviewEulogy(reviewerUserId, eulogyId, reviewStatus, reviewOpinion);
        return Result.success();
    }

    /**
     * 获取我需要审核的挽联列表
     */
    @GetMapping("/my-pending-reviews")
    public Result<List<Eulogy>> getMyPendingReviews(@RequestAttribute("userId") Long reviewerUserId) {
        List<Eulogy> eulogies = eulogyService.getMyPendingReviews(reviewerUserId);
        return Result.success(eulogies);
    }

    /**
     * 获取挽联墙（全部，不按人筛选）
     */
    @GetMapping("/wall")
    public Result<Object> getEulogyWallAll(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(eulogyService.getEulogyWall(null, pageNum, pageSize));
    }

    /**
     * 获取挽联墙
     */
    @GetMapping("/wall/{targetUserId}")
    public Result<Page<Eulogy>> getEulogyWall(@PathVariable Long targetUserId,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Eulogy> page = eulogyService.getEulogyWall(targetUserId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取挽联详情
     */
    @GetMapping("/{eulogyId}")
    public Result<Map<String, Object>> getEulogyDetail(@PathVariable Long eulogyId) {
        Map<String, Object> detail = eulogyService.getEulogyDetail(eulogyId);
        return Result.success(detail);
    }
}
