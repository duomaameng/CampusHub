package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.dto.request.CancelOrderRequest;
import com.campushub.dto.request.CompleteOrderRequest;
import com.campushub.dto.response.OrderDetailResponse;
import com.campushub.dto.response.OrderItemResponse;
import com.campushub.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResult<OrderItemResponse>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(orderService.listOrders(role, status, keyword, page, size));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrder(orderId));
    }

    @PostMapping("/{orderId}/complete")
    public ApiResponse<Void> submitCompletion(@PathVariable Long orderId,
                                               @RequestBody CompleteOrderRequest request) {
        orderService.submitCompletion(orderId, request.getProofImageId(), request.getNote());
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/confirm-completion")
    public ApiResponse<Void> confirmCompletion(@PathVariable Long orderId) {
        orderService.confirmCompletion(orderId);
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable Long orderId,
                                          @RequestBody CancelOrderRequest request) {
        orderService.cancelOrder(orderId, request.getReason());
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/dispute")
    public ApiResponse<Void> disputeOrder(@PathVariable Long orderId,
                                           @RequestBody CancelOrderRequest request) {
        orderService.disputeOrder(orderId, request.getReason());
        return ApiResponse.success();
    }
}
