package com.jieyou.photography.module.order.vo;

/**
 * 创建订单响应，依据 API 约定 5.4.1：返回订单号与初始状态。
 * 同时返回 id，便于前端直接跳转订单详情。
 */
public record CreateOrderResponse(Long id, String orderNo, String status) {
}
