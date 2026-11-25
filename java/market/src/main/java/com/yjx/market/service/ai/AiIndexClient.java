package com.yjx.market.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调 Python 的 /ai/index 建向量索引。
 * <p>
 * 失败策略：吞掉异常，只记 WARN。
 * 因为索引失败只意味着「语义搜不到这件商品」，商品本身还在售、关键词搜索仍然有效。
 * 为一个旁路功能抛异常把上架事务搞挂，属于本末倒置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiIndexClient {

    private final RestClient pythonRestClient;

    /**
     * 给单个商品建索引（幂等：Python 侧是 upsert，重复调没问题）。
     */
    public void indexItem(Long itemId, String title, String description) {
        // 用 HashMap 而不是 Map.of：Map.of 不允许 null 值，description 可能是 null
        Map<String, Object> item = new HashMap<>();
        item.put("itemId", itemId);
        item.put("title", title);
        item.put("description", description);

        Map<String, Object> body = new HashMap<>();
        body.put("mode", "one");
        body.put("items", List.of(item));

        try {
            String resp = pythonRestClient.post()
                    .uri("/ai/index")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            log.info("商品向量索引已触发：itemId={}, resp={}", itemId, resp);
        } catch (Exception e) {
            // 记 warn 不记 error：这是可容忍的降级，不该触发告警
            log.warn("商品向量索引失败（不影响上架）：itemId={}, err={}", itemId, e.getMessage());
        }
    }
}