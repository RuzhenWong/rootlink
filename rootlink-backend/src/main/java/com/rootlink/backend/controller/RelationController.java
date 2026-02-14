package com.rootlink.backend.controller;

import com.rootlink.backend.common.Result;
import com.rootlink.backend.service.RelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/relation")
public class RelationController {

    @Autowired private RelationService relationService;

    /** 搜索用户（身份证号） */
    @GetMapping("/search")
    public Result<Map<String, Object>> searchUser(
            @RequestAttribute("userId") Long userId,
            @RequestParam String idCard) {
        Map<String, Object> result = relationService.searchUser(userId, idCard);
        if (result == null) return Result.error(404, "未找到该用户，对方可能未开启被搜索权限或未实名");
        return Result.success(result);
    }

    /** 发起关系申请（链式） */
    @PostMapping("/apply")
    public Result<Void> applyRelation(
            @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        Long targetUserId = Long.valueOf(body.get("targetUserId").toString());
        String chainJson = body.get("relationChain").toString();
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        relationService.applyRelation(userId, targetUserId, chainJson, reason);
        return Result.success();
    }

    /** 收到的待处理申请 */
    @GetMapping("/pending-applies")
    public Result<List<Map<String, Object>>> getPendingApplies(@RequestAttribute("userId") Long userId) {
        return Result.success(relationService.getPendingApplies(userId));
    }

    /** 处理申请 */
    @PostMapping("/apply/{applyId}/handle")
    public Result<Void> handleApply(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long applyId,
            @RequestBody Map<String, Object> body) {
        Integer action = Integer.valueOf(body.get("action").toString());
        String rejectReason = body.get("rejectReason") != null ? body.get("rejectReason").toString() : null;
        relationService.handleApply(userId, applyId, action, rejectReason);
        return Result.success();
    }

    /** 我的亲属列表 */
    @GetMapping("/my-relations")
    public Result<List<Map<String, Object>>> getMyRelations(@RequestAttribute("userId") Long userId) {
        return Result.success(relationService.getMyRelations(userId));
    }

    /** 推断待确认的关系 */
    @GetMapping("/inferred-pending")
    public Result<List<Map<String, Object>>> getPendingInferred(@RequestAttribute("userId") Long userId) {
        return Result.success(relationService.getMyPendingInferred(userId));
    }

    /** 确认推断关系 */
    @PostMapping("/inferred/{id}/confirm")
    public Result<Void> confirmInferred(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        relationService.confirmInferred(userId, id);
        return Result.success();
    }

    /** 拒绝推断关系 */
    @DeleteMapping("/inferred/{id}")
    public Result<Void> rejectInferred(@RequestAttribute("userId") Long userId, @PathVariable Long id) {
        relationService.rejectInferred(userId, id);
        return Result.success();
    }

    /** 解除关系 */
    @DeleteMapping("/{relationId}")
    public Result<Void> removeRelation(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long relationId) {
        relationService.removeRelation(userId, relationId);
        return Result.success();
    }
}
