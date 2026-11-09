package com.yjx.market.security;

import com.yjx.market.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：从请求头取 token，验签通过就把身份放进 SecurityContext。
 * 注意：不加 @Component，由 SecurityConfig 手动 new，避免被 Spring Boot 重复注册。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final String headerName;
    private final String prefix;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(headerName);

        // 只在带了 "Bearer " 前缀时才处理；没带就直接放行，交给后面的授权规则判断
        if (header != null && header.startsWith(prefix)) {
            String token = header.substring(prefix.length());
            Claims claims = jwtUtil.parse(token);

            if (claims != null) {
                Long userId = jwtUtil.getUserId(claims);
                String role = jwtUtil.getRole(claims);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                                        // principal
                                null,                                          // credentials
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // 放进本次请求的"身份盒子"
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT 认证通过：userId={}, role={}", userId, role);
            }
        }

        // 无论认证成功与否都要放行——认证失败不在这里拦，
        // 交给 authorizeHttpRequests 的规则决定这个接口需不需要登录
        chain.doFilter(request, response);
    }
}