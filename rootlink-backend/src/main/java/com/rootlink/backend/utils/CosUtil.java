package com.rootlink.backend.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
public class CosUtil {

    @Value("${cos.secret-id}")   private String secretId;
    @Value("${cos.secret-key}")  private String secretKey;
    @Value("${cos.bucket}")      private String bucket;
    @Value("${cos.region}")      private String region;
    @Value("${cos.base-url}")    private String baseUrl;

    /**
     * 上传头像到 COS，返回公开访问 URL
     * key 格式：avatars/{userId}_{uuid}.{ext}
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
            ? original.substring(original.lastIndexOf('.')).toLowerCase() : ".jpg";

        String key = "avatars/" + userId + "_"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ext;

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig config = new ClientConfig(new Region(region));
        COSClient client = new COSClient(cred, config);

        try (InputStream is = file.getInputStream()) {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            meta.setContentType(file.getContentType());

            PutObjectRequest req = new PutObjectRequest(bucket, key, is, meta);
            client.putObject(req);

            String url = baseUrl.endsWith("/")
                ? baseUrl + key : baseUrl + "/" + key;
            log.info("COS 上传成功: key={}, url={}", key, url);
            return url;
        } catch (Exception e) {
            log.error("COS 上传失败 userId={}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("头像上传失败: " + e.getMessage(), e);
        } finally {
            client.shutdown();
        }
    }

    /**
     * 删除旧头像（换头像时清理旧文件，key 从 url 中截取）
     */
    public void deleteByUrl(String url) {
        if (url == null || !url.contains(baseUrl)) return;
        String key = url.replace(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/", "");
        if (key.isBlank()) return;
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        COSClient client = new COSClient(cred, new ClientConfig(new Region(region)));
        try {
            client.deleteObject(bucket, key);
            log.info("COS 旧头像删除: key={}", key);
        } catch (Exception e) {
            log.warn("COS 删除旧头像失败（忽略）: {}", e.getMessage());
        } finally {
            client.shutdown();
        }
    }
}
