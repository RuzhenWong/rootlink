package com.rootlink.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 实名认证服务(Mock实现)
 * 
 * TODO: 实际使用时需要接入真实的实名认证API
 * 推荐服务商:
 * - 阿里云实人认证: https://help.aliyun.com/product/60112.html
 * - 腾讯云人脸核身: https://cloud.tencent.com/product/faceid
 * - 百度AI实名认证: https://ai.baidu.com/tech/face/verify
 */
@Slf4j
@Service
public class RealNameVerifyService {

    /**
     * 三要素验证(姓名 + 身份证号 + 手机号)
     * 
     * @param realName 真实姓名
     * @param idCard   身份证号
     * @param phone    手机号
     * @return 是否验证通过
     */
    public boolean verifyThreeElements(String realName, String idCard, String phone) {
        // TODO: 实际对接第三方实名认证API
        
        // Mock实现: 仅做基础格式校验
        log.info("=== Mock实名认证 ===");
        log.info("姓名: {}", maskName(realName));
        log.info("身份证: {}", maskIdCard(idCard));
        log.info("手机号: {}", maskPhone(phone));
        
        // 基础格式校验
        boolean isValid = validateIdCard(idCard) && validatePhone(phone);
        
        log.info("验证结果: {}", isValid ? "通过" : "失败");
        log.info("==================");

        // 实际使用时的代码示例:
        // try {
        //     // 调用实名认证API
        //     RealNameClient client = new RealNameClient(appKey, appSecret);
        //     VerifyRequest request = new VerifyRequest();
        //     request.setName(realName);
        //     request.setIdCard(idCard);
        //     request.setPhone(phone);
        //     
        //     VerifyResponse response = client.verify(request);
        //     
        //     // 判断结果
        //     return response.isSuccess() && "MATCH".equals(response.getResult());
        // } catch (Exception e) {
        //     log.error("实名认证失败", e);
        //     return false;
        // }

        return isValid;
    }

    /**
     * 校验身份证号格式
     */
    private boolean validateIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return false;
        }
        
        // 简单校验正则
        String regex = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$";
        return idCard.matches(regex);
    }

    /**
     * 校验手机号格式
     */
    private boolean validatePhone(String phone) {
        if (phone == null) {
            return false;
        }
        
        String regex = "^1[3-9]\\d{9}$";
        return phone.matches(regex);
    }

    /**
     * 姓名脱敏
     */
    private String maskName(String name) {
        if (name == null || name.length() == 0) {
            return "";
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 身份证脱敏
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
