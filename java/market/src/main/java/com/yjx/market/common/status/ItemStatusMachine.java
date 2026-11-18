package com.yjx.market.common.status;

import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.result.ResultCode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 商品状态机：把「谁能把 A 改成 B」集中成一张表。
 * <p>
 * 规则来源（Step 9 定稿）：
 * - 卖家只能提交审核 / 下架，不能自己上架
 * - 只有管理员和系统（机审）能让待审核变成在售
 * - 下架商品改完内容要重新提交审核，不能直接上架（否则能绕过审核）
 */
public final class ItemStatusMachine {

    /** actor → (from → 允许的 to 集合) */
    private static final Map<StatusActor, Map<ItemStatus, Set<ItemStatus>>> TABLE = buildTable();

    private ItemStatusMachine() {
        // 工具类，不准 new
    }

    private static Map<StatusActor, Map<ItemStatus, Set<ItemStatus>>> buildTable() {
        Map<StatusActor, Map<ItemStatus, Set<ItemStatus>>> table = new EnumMap<>(StatusActor.class);

        // ---------- 卖家 ----------
        Map<ItemStatus, Set<ItemStatus>> seller = new EnumMap<>(ItemStatus.class);
        seller.put(ItemStatus.DRAFT, EnumSet.of(ItemStatus.PENDING_AUDIT));
        seller.put(ItemStatus.REJECTED, EnumSet.of(ItemStatus.PENDING_AUDIT));
        seller.put(ItemStatus.OFF_SALE, EnumSet.of(ItemStatus.PENDING_AUDIT));
        seller.put(ItemStatus.ON_SALE, EnumSet.of(ItemStatus.OFF_SALE));
        table.put(StatusActor.SELLER, seller);

        // ---------- 管理员 ----------
        Map<ItemStatus, Set<ItemStatus>> admin = new EnumMap<>(ItemStatus.class);
        admin.put(ItemStatus.PENDING_AUDIT,
                EnumSet.of(ItemStatus.ON_SALE, ItemStatus.REJECTED));
        table.put(StatusActor.ADMIN, admin);

        // ---------- 系统 ----------
        Map<ItemStatus, Set<ItemStatus>> system = new EnumMap<>(ItemStatus.class);
        system.put(ItemStatus.PENDING_AUDIT, EnumSet.of(ItemStatus.ON_SALE));  // 机审 LOW 自动上架
        system.put(ItemStatus.ON_SALE, EnumSet.of(ItemStatus.SOLD));           // 下单成功
        table.put(StatusActor.SYSTEM, system);

        return table;
    }

    /** 这个操作方能不能把 from 改成 to */
    public static boolean can(ItemStatus from, ItemStatus to, StatusActor actor) {
        return nextOf(from, actor).contains(to);
    }

    /** 不能改就抛 400，错误信息里带中文状态名，方便直接给用户看 */
    public static void assertCan(ItemStatus from, ItemStatus to, StatusActor actor) {
        if (!can(from, to, actor)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    String.format("商品不能从「%s」变成「%s」", from.getLabel(), to.getLabel()));
        }
    }

    /** 某个状态下，该操作方能去的所有状态（查不到返回空集合，不返回 null） */
    public static Set<ItemStatus> nextOf(ItemStatus from, StatusActor actor) {
        Map<ItemStatus, Set<ItemStatus>> byFrom = TABLE.get(actor);
        if (byFrom == null) {
            return Collections.emptySet();
        }
        Set<ItemStatus> targets = byFrom.get(from);
        return targets == null ? Collections.emptySet() : Collections.unmodifiableSet(targets);
    }
}