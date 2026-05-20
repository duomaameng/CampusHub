package com.campushub.service;

import com.campushub.common.PageResult;
import com.campushub.dto.response.OrderDetailResponse;
import com.campushub.dto.response.OrderItemResponse;

public interface OrderService {

    PageResult<OrderItemResponse> listOrders(String role, String status, String keyword, int page, int size);

    OrderDetailResponse getOrder(Long orderId);

    void submitCompletion(Long orderId, Long proofImageId, String note);

    void confirmCompletion(Long orderId);

    void cancelOrder(Long orderId, String reason);

    void disputeOrder(Long orderId, String reason);
}
