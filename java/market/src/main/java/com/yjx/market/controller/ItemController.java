package com.yjx.market.controller;

import com.yjx.market.common.result.Result;
import com.yjx.market.common.util.SecurityUtil;
import com.yjx.market.pojo.dto.ItemCreateDTO;
import com.yjx.market.pojo.dto.ItemQueryDTO;
import com.yjx.market.pojo.dto.ItemStatusDTO;
import com.yjx.market.pojo.vo.ItemCardVO;
import com.yjx.market.pojo.vo.ItemDetailVO;
import com.yjx.market.pojo.vo.PageVO;
import com.yjx.market.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    /** 商品列表（游客可看，不需要 token） */
    @GetMapping("/list")
    public Result<PageVO<ItemCardVO>> list(@Valid ItemQueryDTO query) {
        return Result.ok(itemService.page(query));
    }

    /** 商品详情（游客可看；非在售商品只有卖家本人能看） */
    @GetMapping("/{id}")
    public Result<ItemDetailVO> detail(@PathVariable("id") Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserIdOrNull();
        return Result.ok(itemService.detail(id, currentUserId));
    }

}