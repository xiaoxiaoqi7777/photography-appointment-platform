package com.jieyou.photography.module.order.state;

/**
 * 拍摄时间段，DB 存 MORNING / AFTERNOON / FULL_DAY，
 * 中文时段文本由后端映射返回（API 约定 2.7 / O-03）。
 */
public enum ShootSlot {

    MORNING("上午 09:00-12:00"),
    AFTERNOON("下午 13:00-18:00"),
    FULL_DAY("全天 09:00-18:00");

    private final String text;

    ShootSlot(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    /**
     * 按 DB 值返回中文时段；未知值原样返回，避免历史脏数据导致响应失败。
     */
    public static String textOf(String code) {
        if (code == null) {
            return null;
        }
        for (ShootSlot slot : values()) {
            if (slot.name().equals(code)) {
                return slot.text;
            }
        }
        return code;
    }
}
