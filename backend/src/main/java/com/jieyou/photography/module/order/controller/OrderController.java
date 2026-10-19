package com.jieyou.photography.module.order.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.common.api.PageResult;
import com.jieyou.photography.module.order.dto.CreateOrderRequest;
import com.jieyou.photography.module.order.service.OrderService;
import com.jieyou.photography.module.order.state.OrderStatus;
import com.jieyou.photography.module.order.vo.CreateOrderResponse;
import com.jieyou.photography.module.order.vo.OrderDetailVO;
import com.jieyou.photography.module.order.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端订单接口，依据 API 约定 5.4.1~5.4.4（仅 USER，仅本人订单）。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResult<OrderVO>> list(@RequestParam(required = false) OrderStatus status,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(orderService.myOrders(status, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.userDetail(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderVO> cancel(@PathVariable Long id) {
        return ApiResponse.success(orderService.cancel(id));
    }
}
