package com.jieyou.photography.service.order.state;

/**
 * 订单状态枚举，依据 API 约定 4.1：严格限定 8 种，不允许非法状态。
 */
public enum OrderStatus {

    PENDING(false),   //待确认
    CONFIRMED(false),    //已确认（待拍摄）
    REJECTED(true),      //已拒绝
    CANCELLED(true),   //已取消
    SHOOTING_DONE(false),      //拍摄完成（修图中）
    UPLOADED(false),        //精修已上传（待反馈）
    MODIFYING(false),      //修改中
    DELIVERED(true);     //已交付

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
