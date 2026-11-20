package com.yjx.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.oss.OssTemplate;
import com.yjx.market.common.result.ResultCode;
import com.yjx.market.common.status.ItemStatus;
import com.yjx.market.common.status.ItemStatusMachine;
import com.yjx.market.common.status.StatusActor;
import com.yjx.market.mapper.CategoryMapper;
import com.yjx.market.mapper.AuditRecordMapper;
import com.yjx.market.mapper.ItemImageMapper;
import com.yjx.market.mapper.ItemMapper;
import com.yjx.market.mapper.UserMapper;
import com.yjx.market.pojo.dto.ItemCreateDTO;
import com.yjx.market.pojo.dto.ItemQueryDTO;
import com.yjx.market.pojo.entity.Category;
import com.yjx.market.pojo.entity.AuditRecord;
import com.yjx.market.pojo.entity.Item;
import com.yjx.market.pojo.entity.ItemImage;
import com.yjx.market.pojo.entity.User;
import com.yjx.market.pojo.vo.*;
import com.yjx.market.service.ItemService;
import com.yjx.market.service.audit.AuditRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yjx.market.service.audit.AiAuditClient;
import com.yjx.market.service.audit.AuditRecordService;
import com.yjx.market.service.audit.AuditResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {


    /** 这些状态不允许编辑：在售改内容会坑买家，已售改也没意义 */
    private static final Set<ItemStatus> NOT_EDITABLE = EnumSet.of(ItemStatus.ON_SALE, ItemStatus.SOLD);

    private static final int MAX_IMAGES = 5;

    private final AiAuditClient aiAuditClient;
    private final AuditRecordService auditRecordService;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper; // 新增：批量查卖家昵称
    private final OssTemplate ossTemplate;// 新增：给封面签缩略图地址
    private final AuditRecordMapper auditRecordMapper; // 新增：我发布的列表要带驳回理由

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
        // 先按「待审核」落库：审核结论出来之前，不能让它在集市里出现
        item.setStatus(ItemStatus.PENDING_AUDIT.getCode());
        item.setCoverUrl(keys.get(0));
        itemMapper.insert(item);                       // ← 自增 id 在这一刻回填到 item.getId()

        saveImages(item.getId(), keys);

        // ② 机审：异常的兜底是「转人工」，绝不能因为审核服务挂了就放行
        AuditResult result;
        try {
            result = aiAuditClient.moderate(item.getId(), item.getTitle(), item.getDescription());
        } catch (Exception e) {
            log.error("机审调用失败，按高风险转人工：itemId={}", item.getId(), e);
            result = AuditResult.high("机审服务异常，已转人工复核");
        }

        // ③ 低风险自动上架；其余留在待审核，等人工处置
        if (result.autoPass()) {
            ItemStatusMachine.assertCan(ItemStatus.PENDING_AUDIT, ItemStatus.ON_SALE, StatusActor.SYSTEM);
            item.setStatus(ItemStatus.ON_SALE.getCode());
            itemMapper.updateById(item);
        }

        // ④ 留痕：机审结论无论结果如何都要写
        auditRecordService.saveMachine(item.getId(), result);

        log.info("发布商品成功：id={}, sellerId={}, 图片 {} 张, 机审 risk={}, 状态={}",
                item.getId(), sellerId, keys.size(), result.risk(), item.getStatus());
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long sellerId, Long itemId, ItemCreateDTO dto) {
        Item item = getOwnedItem(sellerId, itemId);
        ItemStatus current = ItemStatus.of(item.getStatus());

        if (NOT_EDITABLE.contains(current)) {
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
        if (current == ItemStatus.REJECTED) {
            ItemStatusMachine.assertCan(current, ItemStatus.PENDING_AUDIT, StatusActor.SELLER);
            item.setStatus(ItemStatus.PENDING_AUDIT.getCode());
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

        ItemStatus from = ItemStatus.of(item.getStatus());
        ItemStatus to = ItemStatus.of(targetStatus);

        // 卖家能做的流转全在状态机里：只有「在售 → 下架」和「各种 → 提交审核」
        // 想直接上架会被拒 —— 那必须经过机审或人审
        ItemStatusMachine.assertCan(from, to, StatusActor.SELLER);

        item.setStatus(to.getCode());
        itemMapper.updateById(item);
        log.info("商品状态变更：id={}, {} -> {}", itemId, from.getCode(), to.getCode());
    }



    @Override
    public PageVO<ItemCardVO> page(ItemQueryDTO query) {
        // ① 组装查询条件
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        // 只查在售：待审核 / 已下架 / 已售出都不出现在集市里
        wrapper.eq(Item::getStatus, ItemStatus.ON_SALE.getCode());

        // 类目：传一级类目时要连带它的子类目
        if (query.getCategoryId() != null) {
            wrapper.in(Item::getCategoryId, resolveCategoryIds(query.getCategoryId()));
        }

        // 关键词：标题或描述命中都算
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Item::getTitle, kw)
                    .or()
                    .like(Item::getDescription, kw));
        }

        // 价格区间
        if (query.getMinPrice() != null) {
            wrapper.ge(Item::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Item::getPrice, query.getMaxPrice());
        }

        // ② 排序：白名单，用户输入绝不拼进 SQL
        String sort = query.getSort() == null ? "" : query.getSort();
        switch (sort) {
            case "price_asc" -> wrapper.orderByAsc(Item::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Item::getPrice);
            default -> wrapper.orderByDesc(Item::getCreateTime);
        }
        // 兜底排序：价格相同的商品之间也要有稳定顺序，否则翻页会重复/漏数据
        wrapper.orderByDesc(Item::getId);

        // ③ 分页查询
        Page<Item> pageParam = new Page<>(query.getPage(), query.getSize());
        IPage<Item> result = itemMapper.selectPage(pageParam, wrapper);

        if (result.getRecords().isEmpty()) {
            return PageVO.of(result, List.of());
        }

        // ④ 批量查卖家昵称：一次 IN 查询，避免"12 条商品查 12 次用户"
        List<Long> sellerIds = result.getRecords().stream()
                .map(Item::getSellerId)
                .distinct()
                .toList();
        Map<Long, String> nicknameMap = userMapper.selectBatchIds(sellerIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> u.getNickname() == null || u.getNickname().isBlank()
                                ? u.getUsername()
                                : u.getNickname()));

        // ⑤ 实体 → VO
        List<ItemCardVO> cards = result.getRecords().stream()
                .map(item -> toCard(item, nicknameMap.getOrDefault(item.getSellerId(), "匿名用户")))
                .toList();

        log.info("商品列表查询：page={}, size={}, 命中 {} 条", query.getPage(), query.getSize(), result.getTotal());
        return PageVO.of(result, cards);
    }

    @Override
    public ItemDetailVO detail(Long itemId, Long currentUserId) {
        // ① 查商品
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }

        // ② 可见性：只有「在售」对外公开
        boolean isOwner = currentUserId != null && currentUserId.equals(item.getSellerId());
        if (!ItemStatus.ON_SALE.getCode().equals(item.getStatus()) && !isOwner) {
            // 故意返回 404 而不是 403：别告诉陌生人「这个 id 存在但你没权限」
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        }

        // ③ 图片：按 sort 升序，转成带签名的 VO
        List<ItemImageVO> images = itemImageMapper.selectList(
                        new LambdaQueryWrapper<ItemImage>()
                                .eq(ItemImage::getItemId, itemId)
                                .orderByAsc(ItemImage::getSort))
                .stream()
                .map(img -> {
                    ItemImageVO vo = new ItemImageVO();
                    vo.setUrl(ossTemplate.signedUrl(img.getUrl(), true));          // 原图带水印
                    vo.setThumbUrl(coverThumbUrl(img.getUrl()));                   // 缩略图不带
                    return vo;
                })
                .toList();

        // ④ 卖家信息：只给 4 个字段
        SellerVO seller = null;
        User user = userMapper.selectById(item.getSellerId());
        if (user != null) {
            seller = new SellerVO();
            seller.setId(user.getId());
            seller.setNickname(user.getNickname() == null || user.getNickname().isBlank()
                    ? user.getUsername()
                    : user.getNickname());
            seller.setAvatar(user.getAvatar());
            seller.setCreateTime(user.getCreateTime());
        }

        // ⑤ 类目全路径：「数码 / 平板」
        String categoryName = buildCategoryPath(item.getCategoryId());

        // ⑥ 组装
        ItemDetailVO vo = new ItemDetailVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setDescription(item.getDescription());
        vo.setPrice(item.getPrice());
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setStatus(item.getStatus());
        vo.setCreateTime(item.getCreateTime());
        vo.setImages(images);
        vo.setSeller(seller);
        vo.setOwner(isOwner);

        return vo;
    }

    @Override
    public PageVO<MyItemVO> myItems(Long sellerId, long page, long size, String status) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 50);

        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getSellerId, sellerId)
                .orderByDesc(Item::getCreateTime)
                .orderByDesc(Item::getId);

        // 状态过滤可选；传了非法值 ItemStatus.of 会抛 400，而不是静默返回空列表
        if (status != null && !status.isBlank()) {
            wrapper.eq(Item::getStatus, ItemStatus.of(status).getCode());
        }

        IPage<Item> result = itemMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<Item> items = result.getRecords();
        if (items.isEmpty()) {
            return PageVO.of(result, Collections.emptyList());
        }

        // 被驳回的商品要显示理由，否则卖家不知道该改什么。
        // 一次查出这批商品的人工记录，按 id 倒序后每个商品留最新一条。
        List<Long> rejectedIds = items.stream()
                .filter(i -> ItemStatus.REJECTED.getCode().equals(i.getStatus()))
                .map(Item::getId)
                .toList();
        Map<Long, String> reasonMap = rejectedIds.isEmpty()
                ? Collections.emptyMap()
                : auditRecordMapper.selectList(new LambdaQueryWrapper<AuditRecord>()
                        .in(AuditRecord::getItemId, rejectedIds)
                        .eq(AuditRecord::getType, AuditRecordService.TYPE_MANUAL)
                        .orderByDesc(AuditRecord::getId))
                .stream()
                .collect(Collectors.toMap(
                        AuditRecord::getItemId,
                        r -> r.getReason() == null ? "" : r.getReason(),
                        (a, b) -> a));

        List<MyItemVO> rows = items.stream().map(item -> {
            MyItemVO vo = new MyItemVO();
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setPrice(item.getPrice());
            vo.setCoverThumbUrl(coverThumbUrl(item.getCoverUrl()));
            vo.setStatus(item.getStatus());
            vo.setCreateTime(item.getCreateTime());
            vo.setRejectReason(reasonMap.get(item.getId()));
            return vo;
        }).toList();

        log.info("我发布的查询：sellerId={}, status={}, 命中 {} 条", sellerId, status, result.getTotal());
        return PageVO.of(result, rows);
    }

    // ---------------------------------------------------------------
    // 下面是私有工具方法
    // ---------------------------------------------------------------

    /** 类目展开：一级类目要连带所有子类目，二级类目就它自己 */
    private List<Long> resolveCategoryIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .select(Category::getId)
                        .eq(Category::getParentId, categoryId))
                .forEach(c -> ids.add(c.getId()));
        return ids;
    }

    /** 数据库存的是原图 key，列表卡片用缩略图（省 10 倍流量） */
    private String coverThumbUrl(String coverKey) {
        if (coverKey == null || coverKey.isBlank()) {
            return null;
        }
        String thumbKey = coverKey.replaceAll("\\.jpg$", "_thumb.jpg");
        return ossTemplate.signedUrl(thumbKey, false);
    }

    /** 实体 → 卡片 VO */
    private ItemCardVO toCard(Item item, String sellerNickname) {
        ItemCardVO vo = new ItemCardVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setPrice(item.getPrice());
        vo.setCoverThumbUrl(coverThumbUrl(item.getCoverUrl()));
        vo.setSellerNickname(sellerNickname);
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

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
    /** 类目全路径：子类目往上找父类目，拼成「数码 / 平板」 */
    private String buildCategoryPath(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        Long cursor = categoryId;

        // 最多往上找 3 层，防止数据里有环导致死循环
        for (int depth = 0; depth < 3 && cursor != null && cursor > 0; depth++) {
            Category category = categoryMapper.selectById(cursor);
            if (category == null) {
                break;
            }
            names.add(0, category.getName());   // 头插：父类目排前面
            cursor = category.getParentId();
        }
        return names.isEmpty() ? null : String.join(" / ", names);
    }
}