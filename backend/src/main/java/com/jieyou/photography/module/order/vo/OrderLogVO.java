package com.jieyou.photography.module.order.vo;

import java.time.LocalDateTime;

/**
 * 订单状态时间线，依据 S-01 裁剪：不返回 operatorId 等内部字段。
 */
public record OrderLogVO(String action,
                         String fromStatus,
                         String toStatus,
                         String remark,
                         LocalDateTime createTime) {
}
