package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.dto.order.*;
import com.campushub.enums.OrderStatus;
import com.campushub.service.OrderService;
import com.campushub.vo.order.OrderDetailVO;
import com.campushub.vo.order.OrderItemVO;
import com.campushub.vo.order.ReviewItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResult<OrderItemVO>> list(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size,
                                                     @RequestParam(required = false) String role,
                                                     @RequestParam(required = false) OrderStatus status,
                                                     @RequestParam(required = false) String keyword) {
        return ApiResponse.success(orderService.listOrders(page, size, role, status, keyword));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailVO> get(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrder(orderId));
    }

    @PostMapping("/{orderId}/complete")
    public ApiResponse<Void> complete(@PathVariable Long orderId, @RequestBody(required = false) OrderCompleteRequest request) {
        orderService.completeOrder(orderId, request == null ? new OrderCompleteRequest() : request);
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/confirm-completion")
    public ApiResponse<Void> confirmCompletion(@PathVariable Long orderId) {
        orderService.confirmCompletion(orderId);
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long orderId, @Valid @RequestBody OrderCancelRequest request) {
        orderService.cancelOrder(orderId, request);
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/messages")
    public ApiResponse<Void> sendMessage(@PathVariable Long orderId, @Valid @RequestBody OrderMessageRequest request) {
        orderService.sendMessage(orderId, request);
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/reviews")
    public ApiResponse<Void> submitReview(@PathVariable Long orderId, @Valid @RequestBody ReviewCreateRequest request) {
        orderService.submitReview(orderId, request);
        return ApiResponse.success();
    }

    @GetMapping("/{orderId}/reviews")
    public ApiResponse<List<ReviewItemVO>> reviews(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.listReviews(orderId));
    }
}
