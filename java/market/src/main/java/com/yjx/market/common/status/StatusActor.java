package com.yjx.market.common.status;

/**
 * 谁在操作。同一个状态变化，不同操作方的权限不同：
 * 卖家能提交审核，但只有管理员能审核通过。
 */
public enum StatusActor {
    SELLER,
    ADMIN,
    SYSTEM
}