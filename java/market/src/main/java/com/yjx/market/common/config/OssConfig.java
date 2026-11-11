package com.yjx.market.common.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OssConfig {

    private final OssProperties props;

    /**
     * OSS 客户端：内部维护连接池，线程安全，全应用共用一个。
     * destroyMethod = "shutdown" → 应用关闭时自动释放连接
     */
    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        return new OSSClientBuilder().build(
                props.getEndpoint(),
                props.getAccessKeyId(),
                props.getAccessKeySecret());
    }
}