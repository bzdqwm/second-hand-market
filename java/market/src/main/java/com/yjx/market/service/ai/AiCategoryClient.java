package com.yjx.market.service.ai;

import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.pojo.dto.AiCategoryDTO;
import com.yjx.market.pojo.vo.AiCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 调 Python 的类目识别接口。
 *
 * 和 AiIndexClient 的区别：
 *   AiIndexClient   → 后台任务（上架触发），失败只记 WARN，不打扰用户
 *   AiCategoryClient → 用户在等结果，失败要明确告诉他，所以抛 BusinessException
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCategoryClient {

    private final RestClient pythonRestClient;

    public AiCategoryVO recognize(AiCategoryDTO dto) {
        try {
            return pythonRestClient.post()
                    .uri("/ai/category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .body(AiCategoryVO.class);
        } catch (Exception e) {
            // 用户在发布页等着，不能让页面静默失败
            log.warn("类目识别失败：title={}, err={}", dto.getTitle(), e.getMessage());
            throw new BusinessException("AI 识别暂时不可用，请手动选择类目");
        }
    }
}