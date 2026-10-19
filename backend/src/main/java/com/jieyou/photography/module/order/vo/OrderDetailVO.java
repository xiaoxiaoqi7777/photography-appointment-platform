package com.jieyou.photography.module.order.vo;

import com.jieyou.photography.module.feedback.vo.FeedbackVO;

import java.util.List;

/**
 * 订单详情响应，依据 API 约定 5.4.3/5.4.6：
 * 订单完整信息 + 状态时间线 + 历轮反馈。
 * 照片入口在阶段六补充。
 */
public record OrderDetailVO(OrderVO order,
                            List<OrderLogVO> timeline,
                            List<FeedbackVO> feedbacks) {
}
