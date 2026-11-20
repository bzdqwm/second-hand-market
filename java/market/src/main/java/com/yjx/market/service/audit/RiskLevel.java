package com.yjx.market.service.audit;

/**
 * 风险等级。和 audit_record.risk 字段（varchar(20)）一一对应。
 */
public enum RiskLevel {

    LOW("低风险"),
    MEDIUM("中风险"),
    HIGH("高风险");

    private final String label;

    RiskLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 只有低风险允许自动上架，其余都进人工队列 */
    public boolean isAutoPass() {
        return this == LOW;
    }
}