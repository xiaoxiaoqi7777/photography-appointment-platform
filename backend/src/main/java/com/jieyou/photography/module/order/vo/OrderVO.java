package com.jieyou.photography.module.order.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单列表项响应：订单字段 + 用户名/主题名/套餐名，并补充中文时段文本（O-03）。
 */
public record OrderVO(Long id,
                      String orderNo,
                      Long userId,
                      String username,
                      Long themeId,
                      String themeName,
                      Long packageId,
                      String packageName,
                      LocalDate shootDate,
                      String shootSlot,
                      String shootSlotText,
                      String remark,
                      String status,
                      String rejectReason,
                      BigDecimal price,
                      LocalDateTime deliverTime,
                      LocalDateTime createTime,
                      LocalDateTime updateTime) {
}
