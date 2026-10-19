package com.jieyou.photography.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 拒绝订单请求，依据 API 约定 5.4.8：拒绝原因必填，≤200 字。
 */
public record RejectOrderRequest(@NotBlank(message = "拒绝原因不能为空")
                                 @Size(max = 200, message = "拒绝原因最长 200 个字符")
                                 String rejectReason) {
}
