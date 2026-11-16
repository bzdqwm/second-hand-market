package com.yjx.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.result.ResultCode;
import com.yjx.market.mapper.CategoryMapper;
import com.yjx.market.mapper.ItemImageMapper;
import com.yjx.market.mapper.ItemMapper;
import com.yjx.market.pojo.dto.ItemCreateDTO;
import com.yjx.market.pojo.entity.Item;
import com.yjx.market.pojo.entity.ItemImage;
import com.yjx.market.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private static final String STATUS_PENDING_AUDIT = "PENDING_AUDIT";
    private static final String STATUS_ON_SALE = "ON_SALE";
    private static final String STATUS_OFF_SALE = "OFF_SALE";
    private static final String STATUS_REJECTED = "REJECTED";

    /** 这些状态不允许编辑：在售改内容会坑买家，已售改也没意义 */
    private static final Set<String> NOT_EDITABLE = Set.of(STATUS_ON_SALE, "SOLD");

    private static final int MAX_IMAGES = 5;

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long sellerId, ItemCreateDTO dto) {
        List<String> keys = checkImages(dto.getImages());
        checkCategory(dto.getCategoryId());

        Item item = new Item();
        item.setSellerId(sellerId);
        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setCategoryId(dto.getCategoryId());
        item.setPrice(dto.getPrice());
        item.setStatus(STATUS_PENDING_AUDIT);
        item.setCoverUrl(keys.get(0));
        itemMapper.insert(item);

        saveImages(item.getId(), keys);

        log.info("发布商品成功：id={}, sellerId={}, 图片 {} 张", item.getId(), sellerId, keys.size());
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long sellerId, Long itemId, ItemCreateDTO dto) {
        Item item = getOwnedItem(sellerId, itemId);

        if (NOT_EDITABLE.contains(item.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "在售或已售商品不能编辑，请先下架");
        }

        List<String> keys = checkImages(dto.getImages());
        checkCategory(dto.getCategoryId());

        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setCategoryId(dto.getCategoryId());
        item.setPrice(dto.getPrice());
        item.setCoverUrl(keys.get(0));

        // 被驳回的商品改完了要重新走审核
        if (STATUS_REJECTED.equals(item.getStatus())) {
            item.setStatus(STATUS_PENDING_AUDIT);
        }
        itemMapper.updateById(item);

        // 图片直接重建：先清空旧的，再按新顺序写入
        itemImageMapper.delete(new LambdaQueryWrapper<ItemImage>()
                .eq(ItemImage::getItemId, itemId));
        saveImages(itemId, keys);

        log.info("编辑商品成功：id={}, sellerId={}, 图片 {} 张", itemId, sellerId, keys.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long sellerId, Long itemId, String targetStatus) {
        Item item = getOwnedItem(sellerId, itemId);
        String current = item.getStatus();

        if (STATUS_ON_SALE.equals(targetStatus)) {
            // ⚠️ 允许 PENDING_AUDIT 上架是过渡方案：审核（Step 9）上线后必须删掉这个条件
            boolean allowed = STATUS_OFF_SALE.equals(current) || STATUS_PENDING_AUDIT.equals(current);
            if (!allowed) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "当前状态不能上架");
            }
        } else if (STATUS_OFF_SALE.equals(targetStatus)) {
            if (!STATUS_ON_SALE.equals(current)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "只有已上架的商品才能下架");
            }
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只支持 ON_SALE / OFF_SALE");
        }

        item.setStatus(targetStatus);
        itemMapper.updateById(item);
        log.info("商品状态变更：id={}, {} -> {}", itemId, current, targetStatus);
    }

    // ---------------------------------------------------------------
    // 下面是三个复用方法，create / update 共用
    // ---------------------------------------------------------------

    /** 校验图片列表：去空白、去重、限数量、只认本平台 key */
    private List<String> checkImages(List<String> images) {
        List<String> keys = images == null
                ? List.of()
                : images.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (keys.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少上传 1 张图片");
        }
        if (keys.size() > MAX_IMAGES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "最多 5 张图片");
        }
        // 只认本平台上传的 objectKey，防止前端塞外部网址
        for (String key : keys) {
            if (!key.startsWith("items/")) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "图片地址不合法");
            }
        }
        return keys;
    }

    /** 类目必须真实存在 */
    private void checkCategory(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "类目不存在");
        }
    }

    /** 查商品 + 校验归属，两个方法共用 */
    private Item getOwnedItem(Long sellerId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人商品");
        }
        return item;
    }

    /** 按顺序写图片，sort 0 是封面 */
    private void saveImages(Long itemId, List<String> keys) {
        for (int i = 0; i < keys.size(); i++) {
            ItemImage image = new ItemImage();
            image.setItemId(itemId);
            image.setUrl(keys.get(i));
            image.setSort(i);
            itemImageMapper.insert(image);
        }
    }
}