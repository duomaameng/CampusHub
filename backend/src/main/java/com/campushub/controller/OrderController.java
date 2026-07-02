package com.campushub.controller;

import com.campushub.common.ApiResponse;
import com.campushub.common.PageResult;
import com.campushub.dto.order.*;
import com.campushub.enums.OrderStatus;
import com.campushub.entity.FileRecord;
import com.campushub.service.FileService;
import com.campushub.service.OrderService;
import com.campushub.vo.order.OrderDetailVO;
import com.campushub.vo.order.OrderItemVO;
import com.campushub.vo.order.OrderStatusLogVO;
import com.campushub.vo.order.ReviewItemVO;
import com.campushub.vo.UnreadCountVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final FileService fileService;

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

    @PostMapping("/{orderId}/cancel-request/approve")
    public ApiResponse<Void> approveCancelRequest(@PathVariable Long orderId) {
        orderService.approveCancelRequest(orderId);
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/cancel-request/reject")
    public ApiResponse<Void> rejectCancelRequest(@PathVariable Long orderId) {
        orderService.rejectCancelRequest(orderId);
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/messages")
    public ApiResponse<Void> sendMessage(@PathVariable Long orderId, @Valid @RequestBody OrderMessageRequest request) {
        orderService.sendMessage(orderId, request);
        return ApiResponse.success();
    }

    @GetMapping("/messages/unread-count")
    public ApiResponse<UnreadCountVO> unreadMessageCount() {
        return ApiResponse.success(new UnreadCountVO(orderService.countUnreadMessages()));
    }

    @PatchMapping("/{orderId}/messages/read")
    public ApiResponse<Void> markMessagesRead(@PathVariable Long orderId) {
        orderService.markMessagesRead(orderId);
        return ApiResponse.success();
    }

    @GetMapping("/{orderId}/messages")
    public ApiResponse<List<com.campushub.vo.order.OrderMessageVO>> messages(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.listMessages(orderId));
    }

    @GetMapping("/{orderId}/messages/{messageId}/attachment")
    public ResponseEntity<Resource> downloadMessageAttachment(@PathVariable Long orderId, @PathVariable Long messageId) {
        FileRecord file = orderService.requireMessageAttachment(orderId, messageId);
        Path path = fileService.resolveStoredFile(file);
        String contentType;
        try {
            contentType = Files.probeContentType(path);
        } catch (Exception ignored) {
            contentType = null;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new FileSystemResource(path));
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

    @GetMapping("/{orderId}/status-logs")
    public ApiResponse<List<OrderStatusLogVO>> statusLogs(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.listStatusLogs(orderId));
    }
}
