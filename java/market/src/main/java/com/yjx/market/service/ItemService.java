package com.yjx.market.service;

import com.yjx.market.pojo.dto.ItemCreateDTO;

/**
 * 商品服务。
 */
public interface ItemService {

    /**
     * 发布商品。
     *
     * @param sellerId 卖家 ID（由 Controller 从 token 解析，不从请求体取）
     * @param dto      表单数据
     * @return 新商品 ID
     */
    Long create(Long sellerId, ItemCreateDTO dto);
}