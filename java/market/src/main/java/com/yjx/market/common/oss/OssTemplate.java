package com.yjx.market.common.oss;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.yjx.market.common.config.OssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Date;

/**
 * OSS 操作封装：上传、生成签名地址、删除。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssTemplate {

    private final OSS ossClient;
    private final OssProperties props;

    /** 上传字节数组到私有桶，返回 objectKey（不是 URL，私有桶地址必须现场签名） */
    public String upload(byte[] bytes, String objectKey, String contentType) {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(bytes.length);
        meta.setContentType(contentType);
        // 告诉浏览器这是图片，避免某些场景被当成下载
        meta.setContentDisposition("inline");

        PutObjectRequest request = new PutObjectRequest(
                props.getBucket(),
                objectKey,
                new ByteArrayInputStream(bytes), meta);

        ossClient.putObject(request);

        log.info("OSS 上传成功：bucket={}, key={}, size={}KB",
                props.getBucket(), objectKey, bytes.length / 1024);
        return objectKey;
    }

    /**
     * 生成签名 URL（有效期取 oss.sign-expire-minutes）。
     *
     * @param objectKey 对象路径
     * @param watermark 是否叠加水印样式
     */
    public String signedUrl(String objectKey, boolean watermark) {
        Date expiration = new Date(
                System.currentTimeMillis() + props.getSignExpireMinutes() * 60L * 1000L);

        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(props.getBucket(), objectKey, HttpMethod.GET);
        request.setExpiration(expiration);

        // ★ 关键：x-oss-process 是 OSS 的「子资源」，参与签名计算。
        //   绝不能先生成签名再手工拼 "&x-oss-process=..."，
        //   私有桶这样访问会返回 SignatureDoesNotMatch。
        //   必须用 setProcess 交给 SDK 一起签。
        if (watermark && StringUtils.hasText(props.getWatermarkStyle())) {
            request.setProcess("style/" + props.getWatermarkStyle());
        }

        String url = ossClient.generatePresignedUrl(request).toString();
        log.debug("生成签名地址：key={}, watermark={}", objectKey, watermark);
        return url;
    }

    /** 服务器内部删除对象（业务失败回滚用） */
    public void delete(String objectKey) {
        ossClient.deleteObject(props.getBucket(), objectKey);
        log.info("OSS 删除：key={}", objectKey);
    }
}