package com.rootlink.backend.controller;

import com.rootlink.backend.common.Result;
import com.rootlink.backend.entity.Testament;
import com.rootlink.backend.service.TestamentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 生前遗言 Controller
 */
@Slf4j
@RestController
@RequestMapping("/v1/testament")
public class TestamentController {

    @Autowired
    private TestamentService testamentService;

    /** 创建遗言 */
    @PostMapping
    public Result<Testament> create(@RequestAttribute("userId") Long userId,
                                              @RequestBody Map<String, Object> body) {
        Testament t = testamentService.create(userId, body);
        return Result.success(t);
    }

    /** 更新遗言 */
    @PutMapping("/{id}")
    public Result<Void> update(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id,
                               @RequestBody Map<String, Object> body) {
        testamentService.update(userId, id, body);
        return Result.success();
    }

    /** 删除遗言 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id) {
        testamentService.delete(userId, id);
        return Result.success();
    }

    /** 我的遗言列表 */
    @GetMapping("/my")
    public Result<List<Testament>> getMyList(@RequestAttribute("userId") Long userId) {
        return Result.success(testamentService.getMyList(userId));
    }

    /** 遗言详情（含接收人信息） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@RequestAttribute("userId") Long userId,
                                                  @PathVariable Long id) {
        return Result.success(testamentService.getDetail(userId, id));
    }
}
