package com.yjx.market.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析。
 */
@Slf4j
@Component
public class JwtUtil {

    /** 签名密钥，构造时由配置的 secret 生成 */
    private final SecretKey key;
    /** token 有效期（毫秒） */
    private final long expire;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire}") long expire) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = expire;
        log.info("JwtUtil 初始化完成，token 有效期 {} 毫秒", expire);
    }

    /**
     * 签发 token。
     *
     * @param userId   用户 id，放入标准字段 sub
     * @param username 用户名，自定义字段
     * @param role     角色，自定义字段
     */
    public String generate(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))          // sub：标准字段，存用户 id
                .claim("username", username)              // 自定义字段
                .claim("role", role)                      // 自定义字段
                .issuedAt(now)                            // 签发时间
                .expiration(new Date(now.getTime() + expire))  // 过期时间
                .signWith(key)                            // 签名
                .compact();                               // 压成字符串
    }

    /**
     * 解析 token。
     *
     * @return 解析成功返回 Claims；签名不对 / 过期 / 格式错 一律返回 null
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)          // 用同一个密钥验签
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("token 解析失败：{}", e.getMessage());
            return null;
        }
    }

    /** 从 Claims 里取用户 id */
    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /** 从 Claims 里取角色 */
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}