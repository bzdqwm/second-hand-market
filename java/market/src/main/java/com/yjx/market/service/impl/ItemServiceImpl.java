package com.yjx.market.service.impl;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    /** 发布后直接进入待审核（审核流程 Step 9 再补） */
    private static final String STATUS_PENDING_AUDIT = "PENDING_AUDIT";

    /** 一个商品最多 5 张图 */
    private static final int MAX_IMAGES = 5;

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long sellerId, ItemCreateDTO dto) {

        // ① 清洗图片列表：去空、去重、限数量
        List<String> keys = dto.getImages() == null
                ? List.of()
                : dto.getImages().stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .distinct()          // 同一个 key 传两次只留一个
                .toList();

        if (keys.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少上传 1 张图片");
        }
        if (keys.size() > MAX_IMAGES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "最多 5 张图片");
        }

        // ② 类目必须真实存在（表里没建外键约束，只能代码兜）
        if (categoryMapper.selectById(dto.getCategoryId()) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "类目不存在");
        }

        // ③ 组装并插入 item
        Item item = new Item();
        item.setSellerId(sellerId);            // 来自 token，前端管不着
        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setCategoryId(dto.getCategoryId());
        item.setPrice(dto.getPrice());
        item.setStatus(STATUS_PENDING_AUDIT);  // 绝不让前端指定
        item.setCoverUrl(keys.get(0));         // 第一张作封面

        itemMapper.insert(item);

        // ④ 写 item_image：sort 从 0 开始，0 就是封面
        for (int i = 0; i < keys.size(); i++) {
            ItemImage image = new ItemImage();
            image.setItemId(item.getId());     // 上一步 insert 后已回填主键
            image.setUrl(keys.get(i));
            image.setSort(i);
            itemImageMapper.insert(image);
        }

        log.info("发布商品成功：id={}, sellerId={}, 图片 {} 张",
                item.getId(), sellerId, keys.size());
        return item.getId();
    }
}