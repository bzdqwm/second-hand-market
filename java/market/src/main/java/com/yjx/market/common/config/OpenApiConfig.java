package com.yjx.market.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置：给 Knife4j / Swagger 提供文档元信息。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI marketOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("二手交易平台 API")
                .description("Spring Boot + Vue3 + FastAPI 三端项目")
                .version("v0.1")
                .contact(new Contact().name("曾煜东")));
    }
}