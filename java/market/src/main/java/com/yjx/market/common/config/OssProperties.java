package com.yjx.market.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置（对应 application.yaml 的 oss: 段）
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /** 地域节点 */
    private String endpoint;

    /** 桶名 */
    private String bucket;

    /** AccessKey ID */
    private String accessKeyId;

    /** AccessKey Secret */
    private String accessKeySecret;

    /** 水印样式名（OSS 控制台里建的） */
    private String watermarkStyle;

    /** 签名 URL 有效期（分钟） */
    private Integer signExpireMinutes;
}