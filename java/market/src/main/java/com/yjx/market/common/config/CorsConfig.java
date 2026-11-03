package com.yjx.market.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域配置。
 * 注意：用 CorsConfigurationSource 而不是 WebMvcConfigurer，
 * 这样 Spring Security 能接管预检请求，不会被鉴权拦掉。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // 允许的前端来源（开发期放行本机所有端口，上线后改成正式域名）
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        // 允许的 HTTP 方法
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 允许的请求头（* 表示全部，包括 Authorization、Content-Type）
        cfg.setAllowedHeaders(List.of("*"));

        // 允许前端 JS 读取的响应头 —— 不加这行，前端读不到 X-Trace-Id
        cfg.setExposedHeaders(List.of("X-Trace-Id"));

        // 允许携带 Cookie（以后如果改用 Cookie 存 token 就需要）
        cfg.setAllowCredentials(true);

        // 预检结果缓存 1 小时，减少 OPTIONS 请求次数
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);   // 对所有路径生效
        return source;
    }
}