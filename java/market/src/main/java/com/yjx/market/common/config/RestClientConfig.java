package com.yjx.market.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Java 调 Python 的 HTTP 客户端。
 * RestClient 是 Spring 6.1 引入的同步客户端，比 RestTemplate 新，写法更顺。
 */
@Configuration
public class RestClientConfig {

    @Value("${ai.python.base-url}")
    private String pythonBaseUrl;

    @Bean
    public RestClient pythonRestClient() {
        return RestClient.builder()
                .baseUrl(pythonBaseUrl)
                .build();
    }
}