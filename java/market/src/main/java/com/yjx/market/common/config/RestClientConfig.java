package com.yjx.market.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

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
        // 必须锁死 HTTP/1.1：
        // JDK HttpClient 默认会先发 "Upgrade: h2c" 想升级到 HTTP/2，
        // 而 uvicorn 不支持 h2c 升级，收到这种请求会把 body 丢掉，
        // 结果 FastAPI 报 422 "body Field required"（明明发了 body）。
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        // 读超时：防止 Python 卡住把请求线程一直占着（完整超时/重试在 Step 12 加固）
        factory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(pythonBaseUrl)
                .requestFactory(factory)
                .build();
    }
}