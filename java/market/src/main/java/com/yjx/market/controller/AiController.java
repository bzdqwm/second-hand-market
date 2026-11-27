package com.yjx.market.controller;

import com.yjx.market.common.result.Result;
import com.yjx.market.pojo.dto.AiCategoryDTO;
import com.yjx.market.pojo.vo.AiCategoryVO;
import com.yjx.market.service.ai.AiCategoryClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiCategoryClient aiCategoryClient;

    /** 根据标题猜类目。发布页输入标题时调。 */
    @PostMapping("/category")
    public Result<AiCategoryVO> category(@Valid @RequestBody AiCategoryDTO dto) {
        return Result.ok(aiCategoryClient.recognize(dto));
    }
}