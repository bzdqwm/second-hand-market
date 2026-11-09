package com.yjx.market.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yjx.market.common.result.Result;
import com.yjx.market.common.result.ResultCode;
import com.yjx.market.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${jwt.header}")
    private String headerName;

    @Value("${jwt.prefix}")
    private String prefix;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtUtil, headerName, prefix);

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 不创建 session：每个请求都靠 token 自带身份
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        // 浏览器跨域预检请求必须放行，否则前端所有跨域请求都会失败
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 登录注册：还没 token，必须公开
                        .requestMatchers("/api/auth/**").permitAll()
                        // 探活与类目：公开
                        .requestMatchers("/api/ping", "/api/ping-ai").permitAll()
                        .requestMatchers("/api/category/**").permitAll()
                        // 接口文档：公开
                        .requestMatchers("/doc.html", "/webjars/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/favicon.ico").permitAll()
                        // 游客白名单：商品列表 / 详情只读
                        .requestMatchers(HttpMethod.GET, "/api/item/list", "/api/item/*").permitAll()
                        // 管理后台：必须 ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 其余全部需要登录
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::onUnauthenticated)   // 401：没登录
                        .accessDeniedHandler(this::onForbidden))            // 403：登录了但没权限
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 未登录 / token 无效 */
    private void onUnauthenticated(HttpServletRequest req, HttpServletResponse res,
                                   AuthenticationException e) throws IOException {
        writeJson(res, ResultCode.UNAUTHORIZED, "未登录或登录已过期");
    }

    /** 已登录但权限不够 */
    private void onForbidden(HttpServletRequest req, HttpServletResponse res,
                             AccessDeniedException e) throws IOException {
        writeJson(res, ResultCode.FORBIDDEN, "没有权限访问该资源");
    }

    /** 按项目约定返回：HTTP 恒 200，业务码看 code */
    private void writeJson(HttpServletResponse res, ResultCode rc, String msg) throws IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(Result.fail(rc, msg)));
    }
}