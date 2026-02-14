package com.rootlink.backend.service;

import com.rootlink.backend.common.ErrorCode;
import com.rootlink.backend.exception.BusinessException;
import com.rootlink.backend.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信服务(Mock实现)
 * 
 * TODO: 实际使用时需要接入真实的短信服务商API
 * 推荐: 阿里云短信服务、腾讯云短信服务等
 */
@Slf4j
@Service
public class SmsService {

    @Autowired
    private RedisUtil redisUtil;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_LIMIT_PREFIX = "sms:limit:";
    private static final int CODE_EXPIRE_SECONDS = 300; // 5分钟
    private static final int SEND_LIMIT_SECONDS = 60; // 1分钟内只能发送一次

    /**
     * 发送验证码
     */
    public void sendCode(String phone, Integer type) {
        // 检查发送频率限制
        String limitKey = SMS_LIMIT_PREFIX + phone;
        if (redisUtil.hasKey(limitKey)) {
            throw new BusinessException(ErrorCode.SMS_SEND_TOO_FREQUENT);
        }

        // 生成6位验证码
        String code = generateCode();

        // 存储验证码到Redis
        String codeKey = SMS_CODE_PREFIX + phone;
        redisUtil.set(codeKey, code, CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 设置发送频率限制
        redisUtil.set(limitKey, "1", SEND_LIMIT_SECONDS, TimeUnit.SECONDS);

        // TODO: 实际发送短信
        // 这里是Mock实现,仅打印日志
        log.info("=== Mock发送验证码 ===");
        log.info("手机号: {}", phone);
        log.info("验证码: {}", code);
        log.info("类型: {}", getTypeDesc(type));
        log.info("=====================");

        // 实际使用时的代码示例:
        // try {
        //     // 调用短信服务商API
        //     SmsClient client = new SmsClient(accessKeyId, accessKeySecret);
        //     SendSmsRequest request = new SendSmsRequest();
        //     request.setPhoneNumbers(phone);
        //     request.setSignName("RootLink");
        //     request.setTemplateCode("SMS_123456");
        //     request.setTemplateParam("{\"code\":\"" + code + "\"}");
        //     
        //     SendSmsResponse response = client.sendSms(request);
        //     if (!"OK".equals(response.getCode())) {
        //         throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        //     }
        // } catch (Exception e) {
        //     log.error("短信发送失败", e);
        //     throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        // }
    }

    /**
     * 验证验证码
     */
    public boolean verifyCode(String phone, String code) {
        String codeKey = SMS_CODE_PREFIX + phone;
        String storedCode = redisUtil.getString(codeKey);
        
        if (storedCode == null) {
            return false;
        }

        boolean isValid = storedCode.equals(code);
        
        // 验证成功后删除验证码
        if (isValid) {
            redisUtil.delete(codeKey);
        }

        return isValid;
    }

    /**
     * 生成6位验证码
     */
    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * 获取类型描述
     */
    private String getTypeDesc(Integer type) {
        switch (type) {
            case 1:
                return "注册";
            case 2:
                return "登录";
            case 3:
                return "重置密码";
            default:
                return "未知";
        }
    }
}
