package com.yjx.market.service;

import com.yjx.market.pojo.dto.ItemCreateDTO;
import com.yjx.market.pojo.dto.ItemQueryDTO;
import com.yjx.market.pojo.vo.ItemCardVO;
import com.yjx.market.pojo.vo.ItemDetailVO;
import com.yjx.market.pojo.vo.MyItemVO;
import com.yjx.market.pojo.vo.PageVO;

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
    /**
     * 编辑商品。只能改自己的，且在售/已售状态不允许编辑。
     */
    void update(Long sellerId, Long itemId, ItemCreateDTO dto);

    /**
     * 上架 / 下架。
     *
     * @param targetStatus 只接受 ON_SALE 或 OFF_SALE
     */
    void changeStatus(Long sellerId, Long itemId, String targetStatus);
    /**
     * 商品分页搜索。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageVO<ItemCardVO> page(ItemQueryDTO query);

    /**
     * 商品详情。
     *
     * @param itemId        商品 ID
     * @param currentUserId 当前登录用户 ID，游客传 null
     */
    ItemDetailVO detail(Long itemId, Long currentUserId);

    /**
     * 我发布的商品（所有状态都能看到，包括被驳回的和已下架的）。
     *
     * @param sellerId 当前登录用户 ID
     * @param page     页码，从 1 开始
     * @param size     每页条数
     * @param status   可选的状态过滤，null 表示全部
     */
    PageVO<MyItemVO> myItems(Long sellerId, long page, long size, String status);
}