package com.yjx.market.controller;

import com.yjx.market.common.result.Result;
import com.yjx.market.common.util.SecurityUtil;
import com.yjx.market.pojo.dto.ItemCreateDTO;
import com.yjx.market.pojo.dto.ItemStatusDTO;
import com.yjx.market.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品相关接口。
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /** 发布商品，返回新商品 ID */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ItemCreateDTO dto) {
        Long sellerId = SecurityUtil.getCurrentUserId();
        return Result.ok(itemService.create(sellerId, dto));
    }

    /** 编辑商品 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody ItemCreateDTO dto) {
        Long sellerId = SecurityUtil.getCurrentUserId();
        itemService.update(sellerId, id, dto);
        return Result.ok();
    }

    /** 上架 / 下架 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody ItemStatusDTO dto) {
        Long sellerId = SecurityUtil.getCurrentUserId();
        itemService.changeStatus(sellerId, id, dto.getStatus());
        return Result.ok();
    }
}