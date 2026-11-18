package com.yjx.market.common.status;

import com.yjx.market.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态机规则测试。
 * 不用 @SpringBootTest —— 状态机是纯 Java 类，启动整个 Spring 只是浪费时间。
 */
class ItemStatusMachineTest {

    @Test
    @DisplayName("卖家可以下架在售商品")
    void sellerCanTakeOffShelf() {
        assertTrue(ItemStatusMachine.can(ItemStatus.ON_SALE, ItemStatus.OFF_SALE, StatusActor.SELLER));
    }

    @Test
    @DisplayName("卖家不能把下架商品直接上架，必须重新走审核")
    void sellerCannotPutOnSaleDirectly() {
        assertFalse(ItemStatusMachine.can(ItemStatus.OFF_SALE, ItemStatus.ON_SALE, StatusActor.SELLER));
        BusinessException e = assertThrows(BusinessException.class,
                () -> ItemStatusMachine.assertCan(ItemStatus.OFF_SALE, ItemStatus.ON_SALE, StatusActor.SELLER));
        assertEquals("商品不能从「已下架」变成「在售」", e.getMessage());
    }

    @Test
    @DisplayName("卖家不能自己审核自己的商品")
    void sellerCannotApprove() {
        assertFalse(ItemStatusMachine.can(ItemStatus.PENDING_AUDIT, ItemStatus.ON_SALE, StatusActor.SELLER));
    }

    @Test
    @DisplayName("管理员可以通过 / 驳回待审核商品")
    void adminCanReview() {
        assertTrue(ItemStatusMachine.can(ItemStatus.PENDING_AUDIT, ItemStatus.ON_SALE, StatusActor.ADMIN));
        assertTrue(ItemStatusMachine.can(ItemStatus.PENDING_AUDIT, ItemStatus.REJECTED, StatusActor.ADMIN));
    }

    @Test
    @DisplayName("被驳回的商品，卖家可以重新提交")
    void rejectedCanResubmit() {
        assertTrue(ItemStatusMachine.can(ItemStatus.REJECTED, ItemStatus.PENDING_AUDIT, StatusActor.SELLER));
    }

    @Test
    @DisplayName("已售出是终点，谁都不能再改")
    void soldIsFinal() {
        for (StatusActor actor : StatusActor.values()) {
            assertTrue(ItemStatusMachine.nextOf(ItemStatus.SOLD, actor).isEmpty(), actor.name() + " 不该能改已售出商品");
        }
    }

    @Test
    @DisplayName("非法流转被拒绝：已下架不能直接已售出")
    void illegalTransitionRejected() {
        assertFalse(ItemStatusMachine.can(ItemStatus.OFF_SALE, ItemStatus.SOLD, StatusActor.SYSTEM));
        assertThrows(BusinessException.class,
                () -> ItemStatusMachine.assertCan(ItemStatus.OFF_SALE, ItemStatus.SOLD, StatusActor.SYSTEM));
    }

    @Test
    @DisplayName("字符串能正确解析成枚举，非法值报错")
    void parseCode() {
        assertEquals(ItemStatus.ON_SALE, ItemStatus.of("ON_SALE"));
        assertThrows(BusinessException.class, () -> ItemStatus.of("ON_SALES"));
    }
}