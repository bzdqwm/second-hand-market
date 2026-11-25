package com.yjx.market.service.ai;

import com.yjx.market.common.event.ItemOnSaleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 上架 → 建向量索引。
 * <p>
 * 关键是 AFTER_COMMIT：等事务真正提交了才调 Python。
 * 写在事务里的话，HTTP 调用期间会一直占着数据库连接，
 * Python 一慢（模型首次加载要几十秒），连接池就会被拖死。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemIndexListener {

    private final AiIndexClient aiIndexClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onItemOnSale(ItemOnSaleEvent event) {
        log.info("上架事件已提交，开始建索引：itemId={}", event.itemId());
        aiIndexClient.indexItem(event.itemId(), event.title(), event.description());
    }
}