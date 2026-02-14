package com.rootlink.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rootlink.backend.entity.Testament;
import com.rootlink.backend.entity.TestamentReceiver;
import com.rootlink.backend.exception.BusinessException;
import com.rootlink.backend.mapper.TestamentMapper;
import com.rootlink.backend.mapper.TestamentReceiverMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class TestamentService {

    @Autowired private TestamentMapper testamentMapper;
    @Autowired private TestamentReceiverMapper receiverMapper;

    @Transactional(rollbackFor = Exception.class)
    public Testament create(Long userId, Map<String, Object> body) {
        String title = getString(body, "title", "我的遗言");
        String content = getString(body, "content", "");
        Integer testamentType = getInt(body, "testamentType", 1);
        Integer visibility = getInt(body, "visibility", 0);

        if (content.isBlank()) throw new BusinessException(400, "遗言内容不能为空");

        Testament t = new Testament();
        t.setUserId(userId);
        t.setTitle(title);
        t.setContentEncrypted(content);
        t.setTestamentType(testamentType);
        t.setUnlockStatus(0);
        t.setVisibility(visibility);
        t.setVisibilityNote(getString(body, "visibilityNote", null));
        testamentMapper.insert(t);

        // 如果是指定人（type=2），保存接收人列表
        if (testamentType == 2 && body.get("receiverIds") != null) {
            List<?> receiverIds = (List<?>) body.get("receiverIds");
            for (Object rid : receiverIds) {
                TestamentReceiver rec = new TestamentReceiver();
                rec.setTestamentId(t.getId());
                rec.setReceiverUserId(Long.valueOf(rid.toString()));
                rec.setReceiverType(0);
                rec.setHasRead(0);
                receiverMapper.insert(rec);
            }
        }

        // 财产分配公开（type=3, visibility=3）：直系两代自动加为接收人（visibility=2时后续解锁再处理）

        log.info("遗言已创建: userId={}, id={}, type={}, visibility={}", userId, t.getId(), testamentType, visibility);
        return t;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, Map<String, Object> body) {
        Testament t = getOwned(userId, id);
        if (body.containsKey("title")) t.setTitle(getString(body, "title", t.getTitle()));
        if (body.containsKey("content")) {
            String c = getString(body, "content", "");
            if (c.isBlank()) throw new BusinessException(400, "遗言内容不能为空");
            t.setContentEncrypted(c);
        }
        if (body.containsKey("testamentType")) t.setTestamentType(getInt(body, "testamentType", t.getTestamentType()));
        if (body.containsKey("visibility")) t.setVisibility(getInt(body, "visibility", t.getVisibility()));
        if (body.containsKey("visibilityNote")) t.setVisibilityNote(getString(body, "visibilityNote", null));
        testamentMapper.updateById(t);

        // 更新指定接收人
        if (body.containsKey("receiverIds")) {
            LambdaQueryWrapper<TestamentReceiver> w = new LambdaQueryWrapper<>();
            w.eq(TestamentReceiver::getTestamentId, id);
            receiverMapper.delete(w);
            List<?> receiverIds = (List<?>) body.get("receiverIds");
            if (receiverIds != null) {
                for (Object rid : receiverIds) {
                    TestamentReceiver rec = new TestamentReceiver();
                    rec.setTestamentId(id);
                    rec.setReceiverUserId(Long.valueOf(rid.toString()));
                    rec.setReceiverType(0);
                    rec.setHasRead(0);
                    receiverMapper.insert(rec);
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        Testament t = getOwned(userId, id);
        t.setDeleted(1);
        testamentMapper.updateById(t);
    }

    public List<Testament> getMyList(Long userId) {
        LambdaQueryWrapper<Testament> w = new LambdaQueryWrapper<>();
        w.eq(Testament::getUserId, userId).eq(Testament::getDeleted, 0).orderByDesc(Testament::getCreateTime);
        return testamentMapper.selectList(w);
    }

    public Map<String, Object> getDetail(Long userId, Long id) {
        Testament t = getOwned(userId, id);
        Map<String, Object> result = new HashMap<>();
        result.put("id", t.getId());
        result.put("title", t.getTitle());
        result.put("content", t.getContentEncrypted());
        result.put("testamentType", t.getTestamentType());
        result.put("visibility", t.getVisibility());
        result.put("visibilityNote", t.getVisibilityNote());
        result.put("unlockStatus", t.getUnlockStatus());
        result.put("createTime", t.getCreateTime());
        result.put("updateTime", t.getUpdateTime());

        // 加载接收人
        LambdaQueryWrapper<TestamentReceiver> rw = new LambdaQueryWrapper<>();
        rw.eq(TestamentReceiver::getTestamentId, id);
        List<TestamentReceiver> receivers = receiverMapper.selectList(rw);
        result.put("receivers", receivers);
        return result;
    }

    private Testament getOwned(Long userId, Long id) {
        Testament t = testamentMapper.selectById(id);
        if (t == null || t.getDeleted() == 1) throw new BusinessException(404, "遗言不存在");
        if (!t.getUserId().equals(userId)) throw new BusinessException(403, "无权操作该遗言");
        return t;
    }

    private String getString(Map<String, Object> b, String k, String def) {
        Object v = b.get(k); return v != null ? v.toString() : def;
    }
    private Integer getInt(Map<String, Object> b, String k, Integer def) {
        Object v = b.get(k); return v != null ? Integer.valueOf(v.toString()) : def;
    }
}
