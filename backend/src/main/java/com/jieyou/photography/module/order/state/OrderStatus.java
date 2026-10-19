package com.jieyou.photography.module.order.state;

/**
 * 订单状态枚举，依据 API 约定 4.1：严格限定 8 种，不允许非法状态。
 */
public enum OrderStatus {

    PENDING(false),
    CONFIRMED(false),
    REJECTED(true),
    CANCELLED(true),
    SHOOTING_DONE(false),
    UPLOADED(false),
    MODIFYING(false),
    DELIVERED(true);

    private final boolean terminal;

    OrderStatus(boolean terminal) {
        this.terminal = terminal;
    }

    /**
     * 终态不可再流转：REJECTED / CANCELLED / DELIVERED。
     */
    public boolean isTerminal() {
        return terminal;
    }
}
