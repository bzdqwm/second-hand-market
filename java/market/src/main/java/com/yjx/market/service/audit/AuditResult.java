package com.yjx.market.service.audit;

import java.math.BigDecimal;

/**
 * 机审结论。对应 audit_record 里的 risk / score / reason 三个字段。
 */
public record AuditResult(RiskLevel risk, BigDecimal score, String reason) {

    /** 紧凑构造器：所有实例都必须过这一道校验 */
    public AuditResult {
        if (risk == null) {
            throw new IllegalArgumentException("risk 不能为空");
        }
        if (score != null
                && (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("score 必须在 0~1 之间：" + score);
        }
    }

    /** 是否可以直接上架 */
    public boolean autoPass() {
        return risk.isAutoPass();
    }

    public static AuditResult low(String reason) {
        return new AuditResult(RiskLevel.LOW, new BigDecimal("0.05"), reason);
    }

    public static AuditResult high(String reason) {
        return new AuditResult(RiskLevel.HIGH, new BigDecimal("0.95"), reason);
    }
}