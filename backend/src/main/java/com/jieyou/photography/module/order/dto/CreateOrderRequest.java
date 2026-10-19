package com.jieyou.photography.module.order.dto;

import com.jieyou.photography.module.order.state.ShootSlot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 提交预约请求，依据 API 约定 5.4.1：
 * themeId/packageId/shootDate/shootSlot 必填，remark 可选且 ≤200 字。
 */
public record CreateOrderRequest(@NotNull(message = "主题不能为空")
                                 Long themeId,
                                 @NotNull(message = "套餐不能为空")
                                 Long packageId,
                                 @NotNull(message = "拍摄日期不能为空")
                                 LocalDate shootDate,
                                 @NotNull(message = "拍摄时间段不能为空")
                                 ShootSlot shootSlot,
                                 @Size(max = 200, message = "备注最长 200 个字符")
                                 String remark) {
}
