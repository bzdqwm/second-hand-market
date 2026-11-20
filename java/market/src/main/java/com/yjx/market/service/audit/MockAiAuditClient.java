package com.yjx.market.service.audit;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 机审假实现：固定 LOW，用来先把「审核 → 上架」这条链路跑通。
 * Step 14 写 HttpAiAuditClient 后，把配置 audit.mode 改成 http 即可切换。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "audit", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockAiAuditClient implements AiAuditClient {

    /** 联调开关：改成 true，所有商品都会被判高风险进人工队列 */
    private static final boolean FORCE_HIGH = false;

    @PostConstruct
    void init() {
        log.warn("【机审】Mock 实现已加载，固定返回 {}（Step 14 换真实 Python 审核）",
                FORCE_HIGH ? "HIGH" : "LOW");
    }

    @Override
    public AuditResult moderate(Long itemId, String title, String description) {
        log.info("【机审】Mock 审核 itemId={}, title={}", itemId, title);
        if (FORCE_HIGH) {
            return AuditResult.high("Mock：强制高风险，用于联调人工审核队列");
        }
        return AuditResult.low("Mock：固定低风险，自动放行");
    }
}