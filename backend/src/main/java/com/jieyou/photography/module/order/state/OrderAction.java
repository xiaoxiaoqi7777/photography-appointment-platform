package com.jieyou.photography.module.order.state;

/**
 * 订单操作动作枚举，依据 API 约定第 6 章：
 * CREATE / CONFIRM / REJECT / CANCEL / SHOOT_DONE / UPLOAD / MODIFY / DELIVER。
 */
public enum OrderAction {

    CREATE,
    CONFIRM,
    REJECT,
    CANCEL,
    SHOOT_DONE,
    UPLOAD,
    MODIFY,
    DELIVER
}
