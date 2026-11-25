package com.yjx.market.service.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.oss.OssTemplate;
import com.yjx.market.common.result.ResultCode;
import com.yjx.market.common.status.ItemStatus;
import com.yjx.market.common.status.ItemStatusMachine;
import com.yjx.market.common.status.StatusActor;
import com.yjx.market.mapper.AuditRecordMapper;
import com.yjx.market.mapper.ItemImageMapper;
import com.yjx.market.mapper.ItemMapper;
import com.yjx.market.mapper.UserMapper;
import com.yjx.market.pojo.dto.AuditHandleDTO;
import com.yjx.market.pojo.entity.AuditRecord;
import com.yjx.market.pojo.entity.Item;
import com.yjx.market.pojo.entity.ItemImage;
import com.yjx.market.pojo.entity.User;
import com.yjx.market.pojo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端审核：待审列表 / 详情 / 处置。
 * <p>
 * 这里只给管理员用，Controller 层已由 SecurityConfig 的 hasRole('ADMIN') 兜住。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final UserMapper userMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final AuditRecordService auditRecordService;
    private final OssTemplate ossTemplate;

    /** 待审列表：只看 PENDING_AUDIT，先提交的先审 */
    public PageVO<AuditItemVO> list(long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        IPage<Item> result = itemMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<Item>()
                        .eq(Item::getStatus, ItemStatus.PENDING_AUDIT.getCode())
                        .orderByAsc(Item::getCreateTime)
                        .orderByAsc(Item::getId));

        List<Item> items = result.getRecords();
        if (items.isEmpty()) {
            return PageVO.of(result, Collections.emptyList());
        }

        List<Long> itemIds = items.stream().map(Item::getId).toList();

        // 一次查出这批商品的机审记录，按 id 倒序后每个商品留第一条（= 最新一条）
        // 不在循环里逐条查库，否则 10 条商品 = 21 次 SQL（N+1）
        Map<Long, AuditRecord> machineMap = auditRecordMapper.selectList(
                        new LambdaQueryWrapper<AuditRecord>()
                                .in(AuditRecord::getItemId, itemIds)
                                .eq(AuditRecord::getType, AuditRecordService.TYPE_MACHINE)
                                .orderByDesc(AuditRecord::getId))
                .stream()
                .collect(Collectors.toMap(AuditRecord::getItemId, Function.identity(), (a, b) -> a));

        List<Long> sellerIds = items.stream().map(Item::getSellerId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectList(
                        new LambdaQueryWrapper<User>().in(User::getId, sellerIds))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AuditItemVO> rows = items.stream().map(item -> {
            AuditItemVO vo = new AuditItemVO();
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setPrice(item.getPrice());
            vo.setCoverThumbUrl(coverThumbUrl(item.getCoverUrl()));
            vo.setCreateTime(item.getCreateTime());
            vo.setSellerNickname(nicknameOf(userMap.get(item.getSellerId())));

            AuditRecord machine = machineMap.get(item.getId());
            if (machine != null) {
                vo.setRisk(machine.getRisk());
                vo.setScore(machine.getScore());
                vo.setMachineReason(machine.getReason());
            }
            return vo;
        }).toList();

        log.info("待审列表查询：page={}, size={}, 共 {} 条待审", safePage, safeSize, result.getTotal());
        return PageVO.of(result, rows);
    }

    /** 审核详情：图文 + 最近一次机审结论 */
    public AuditDetailVO detail(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }

        AuditDetailVO vo = new AuditDetailVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setDescription(item.getDescription());
        vo.setPrice(item.getPrice());
        vo.setCategoryId(item.getCategoryId());
        vo.setStatus(item.getStatus());
        vo.setCreateTime(item.getCreateTime());

        vo.setImages(itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                        .eq(ItemImage::getItemId, itemId)
                        .orderByAsc(ItemImage::getSort))
                .stream()
                .map(img -> {
                    ItemImageVO image = new ItemImageVO();
                    image.setUrl(ossTemplate.signedUrl(img.getUrl(), true));
                    image.setThumbUrl(coverThumbUrl(img.getUrl()));
                    return image;
                }).toList());

        vo.setSeller(sellerOf(userMapper.selectById(item.getSellerId())));

        vo.setMachineRecord(toRecordVO(auditRecordMapper.selectOne(
                new LambdaQueryWrapper<AuditRecord>()
                        .eq(AuditRecord::getItemId, itemId)
                        .eq(AuditRecord::getType, AuditRecordService.TYPE_MACHINE)
                        .orderByDesc(AuditRecord::getId)
                        .last("LIMIT 1"))));

        return vo;
    }

    /** 处置：通过 → 上架；驳回 → 驳回态 */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long operatorId, Long itemId, AuditHandleDTO dto) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }

        ItemStatus current = ItemStatus.of(item.getStatus());
        boolean pass = AuditRecordService.PASS.equals(dto.getResult());
        ItemStatus target = pass ? ItemStatus.ON_SALE : ItemStatus.REJECTED;

        // 只有 PENDING_AUDIT 能被处置：已上架/已下架的点进来会被状态机拦下
        ItemStatusMachine.assertCan(current, target, StatusActor.ADMIN);

        // 驳回必须给理由：卖家得知道改什么
        if (!pass && (dto.getReason() == null || dto.getReason().isBlank())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "驳回必须填写理由");
        }

        item.setStatus(target.getCode());
        itemMapper.updateById(item);

        auditRecordService.saveManual(itemId, operatorId, pass, dto.getReason());

        log.info("人工审核完成：itemId={}, operatorId={}, {} -> {}, result={}",
                itemId, operatorId, current.getCode(), target.getCode(), dto.getResult());
    }

    // ---------------- 私有工具 ----------------

    private String coverThumbUrl(String coverKey) {
        if (coverKey == null || coverKey.isBlank()) {
            return null;
        }
        return ossTemplate.signedUrl(coverKey.replaceAll("\\.jpg$", "_thumb.jpg"), false);
    }

    private String nicknameOf(User user) {
        if (user == null) {
            return "未知用户";
        }
        return user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername()
                : user.getNickname();
    }

    private SellerVO sellerOf(User user) {
        if (user == null) {
            return null;
        }
        SellerVO seller = new SellerVO();
        seller.setId(user.getId());
        seller.setNickname(nicknameOf(user));
        seller.setAvatar(ossTemplate.avatarUrl(user.getAvatar()));
        seller.setCreateTime(user.getCreateTime());
        return seller;
    }

    private AuditRecordVO toRecordVO(AuditRecord record) {
        if (record == null) {
            return null;
        }
        AuditRecordVO vo = new AuditRecordVO();
        vo.setId(record.getId());
        vo.setType(record.getType());
        vo.setMachineResult(record.getMachineResult());
        vo.setResult(record.getResult());
        vo.setRisk(record.getRisk());
        vo.setScore(record.getScore());
        vo.setReason(record.getReason());
        vo.setOperatorId(record.getOperatorId());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}