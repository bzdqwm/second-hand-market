package com.yjx.market.common.util;

import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 里取当前登录用户。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /** 取当前登录用户 id；未登录直接抛 401 */
    public static Long getCurrentUserId() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }

    /** 取当前登录用户 id；游客返回 null（用于"登录可看更多"的场景） */
    public static Long getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    /** 取当前用户角色；游客返回 null */
    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        }
        return null;
    }
}