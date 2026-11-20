package com.yjx.market.service.audit;

/**
 * 机审客户端。实现方负责把商品内容送去审核，拿回风险结论。
 * <p>
 * 约定：
 * 1. 实现内部自己处理超时 / 重试，不要抛业务异常给调用方；
 * 2. 实在拿不到结论时，返回 {@link AuditResult#high(String)}（宁可转人工，不可放过）；
 * 3. 结果不落库 —— 落库是调用方的责任（见 AuditService）。
 */
public interface AiAuditClient {

    /**
     * @param itemId      商品 ID（日志用，Python 侧也会回传）
     * @param title       标题
     * @param description 描述
     */
    AuditResult moderate(Long itemId, String title, String description);
}