package com.yjx.market.service.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MockAiAuditClientTest {

    private final MockAiAuditClient client = new MockAiAuditClient();

    @Test
    @DisplayName("Mock 永远返回低风险，且可直接上架")
    void alwaysLowRisk() {
        AuditResult r = client.moderate(1L, "iPhone 14", "九成新，无划痕");
        assertEquals(RiskLevel.LOW, r.risk());
        assertTrue(r.autoPass());
        assertEquals(0, r.score().compareTo(new BigDecimal("0.05")));
        assertNotNull(r.reason());
    }

    @Test
    @DisplayName("AuditResult 拒绝越界的风险分")
    void rejectInvalidScore() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuditResult(RiskLevel.LOW, new BigDecimal("1.5"), "越界"));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditResult(null, new BigDecimal("0.1"), "没等级"));
    }

    @Test
    @DisplayName("非 LOW 一律不进自动放行")
    void onlyLowAutoPass() {
        assertFalse(RiskLevel.MEDIUM.isAutoPass());
        assertFalse(RiskLevel.HIGH.isAutoPass());
        assertEquals("高风险", RiskLevel.HIGH.getLabel());
    }
}