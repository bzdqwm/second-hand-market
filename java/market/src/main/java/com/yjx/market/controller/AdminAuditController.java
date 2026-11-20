package com.yjx.market.controller;

import com.yjx.market.common.result.Result;
import com.yjx.market.common.util.SecurityUtil;
import com.yjx.market.pojo.dto.AuditHandleDTO;
import com.yjx.market.pojo.vo.AuditDetailVO;
import com.yjx.market.pojo.vo.AuditItemVO;
import com.yjx.market.pojo.vo.PageVO;
import com.yjx.market.service.audit.AdminAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端审核台接口。整个 /api/admin/** 已由 SecurityConfig 限定 ADMIN。
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    /** 待审列表 */
    @GetMapping("/list")
    public Result<PageVO<AuditItemVO>> list(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(adminAuditService.list(page, size));
    }

    /** 审核详情 */
    @GetMapping("/{itemId}")
    public Result<AuditDetailVO> detail(@PathVariable("itemId") Long itemId) {
        return Result.ok(adminAuditService.detail(itemId));
    }

    /** 处置：PASS 通过 / REJECT 驳回 */
    @PutMapping("/{itemId}")
    public Result<Void> handle(@PathVariable("itemId") Long itemId,
                               @Valid @RequestBody AuditHandleDTO dto) {
        adminAuditService.handle(SecurityUtil.getCurrentUserId(), itemId, dto);
        return Result.ok();
    }
}