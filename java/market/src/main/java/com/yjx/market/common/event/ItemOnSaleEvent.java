package com.yjx.market.common.event;

/**
 * 商品上架事件。
 * <p>
 * 为什么用事件而不是直接在 create() 里调 Python：
 * 上架是主流程，建索引是旁路。用事件把两者解耦后，
 * 将来加「上架发通知」「上架刷缓存」都只是再挂一个监听器，不用再改商品服务。
 *
 * @param itemId      商品 ID
 * @param title       标题（送给 embedding）
 * @param description 描述（送给 embedding，可为空）
 */
public record ItemOnSaleEvent(Long itemId, String title, String description) {
}