package com.jieyou.photography.module.order.controller;

import com.jieyou.photography.common.api.ApiResponse;
import com.jieyou.photography.common.api.PageResult;
import com.jieyou.photography.module.order.dto.RejectOrderRequest;
import com.jieyou.photography.module.order.service.OrderService;
import com.jieyou.photography.module.order.state.OrderStatus;
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
 * 管理端订单接口，依据 API 约定 5.4.5~5.4.9（ADMIN）。
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResult<OrderVO>> list(@RequestParam(required = false) OrderStatus status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(orderService.adminOrders(status, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.adminDetail(id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<OrderVO> confirm(@PathVariable Long id) {
        return ApiResponse.success(orderService.adminConfirm(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<OrderVO> reject(@PathVariable Long id,
                                       @Valid @RequestBody RejectOrderRequest request) {
        return ApiResponse.success(orderService.adminReject(id, request));
    }

    @PostMapping("/{id}/shoot-done")
    public ApiResponse<OrderVO> shootDone(@PathVariable Long id) {
        return ApiResponse.success(orderService.adminShootDone(id));
    }
}
