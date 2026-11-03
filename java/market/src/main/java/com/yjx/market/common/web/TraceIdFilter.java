package com.yjx.market.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 给每个请求分配 traceId，放进 MDC，日志里就能带上。
 * 响应头也回传 X-Trace-Id，方便前端报障时报编号。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // 最早执行，连被安全拦截的请求也有 ID
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";

    /** 只接受字母数字和横线，防止日志注入（有人塞换行符伪造日志） */
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String traceId = request.getHeader(HEADER);
        if (traceId == null || !VALID.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);

        try {
            chain.doFilter(request, response);   // 业务代码在这一行里跑
        } finally {
            MDC.remove(MDC_KEY);                 // 必须清！Tomcat 线程是复用的
        }
    }
}