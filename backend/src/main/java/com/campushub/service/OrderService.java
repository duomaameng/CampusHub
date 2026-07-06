package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.dto.order.OrderCancelRequest;
import com.campushub.dto.order.OrderCompleteRequest;
import com.campushub.dto.order.ReviewCreateRequest;
import com.campushub.entity.*;
import com.campushub.enums.ApplicationStatus;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.TaskStatus;
import com.campushub.enums.UploadBusinessType;
import com.campushub.mapper.*;
import com.campushub.realtime.RealtimeEventPublisher;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.order.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final int DEFAULT_CREDIT_SCORE = 100;
    private static final int MIN_CREDIT_SCORE = 0;
    private static final int MAX_CREDIT_SCORE = 100;

    private final OrderMapper orderMapper;
    private final TaskMapper taskMapper;
    private final UserProfileMapper userProfileMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final ApplicationMapper applicationMapper;
    private final ReviewMapper reviewMapper;
    private final CreditLogMapper creditLogMapper;
    private final TaskImageMapper taskImageMapper;
    private final TaskFileMapper taskFileMapper;
    private final NotificationService notificationService;
    private final FileService fileService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public PageResult<OrderItemVO> listOrders(int page, int size, String role, OrderStatus status, String keyword) {
        refreshTimedOutOrders();

        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Page<Order> pageQuery = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Order::getStatus, OrderStatus.CANCELLED);

        if ("PUBLISHER".equalsIgnoreCase(role)) {
            wrapper.eq(Order::getPublisherId, currentUserId);
        } else if ("PROVIDER".equalsIgnoreCase(role)) {
            wrapper.eq(Order::getServiceProviderId, currentUserId);
            wrapper.ne(Order::getStatus, OrderStatus.PENDING_CONFIRM);
        } else {
            wrapper.and(w ->
                w.eq(Order::getPublisherId, currentUserId)
                 .or(w2 -> w2.eq(Order::getServiceProviderId, currentUserId)
                              .ne(Order::getStatus, OrderStatus.PENDING_CONFIRM))
            );
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.last("""
                ORDER BY CASE status
                    WHEN 'IN_PROGRESS' THEN 0
                    WHEN 'PENDING_COMPLETION' THEN 0
                    WHEN 'PENDING_CONFIRM' THEN 1
                    WHEN 'DISPUTE' THEN 1
                    WHEN 'COMPLETED' THEN 2
                    WHEN 'REVIEWED' THEN 2
                    ELSE 3
                END, created_at DESC
                """);

        Page<Order> result = orderMapper.selectPage(pageQuery, wrapper);
        List<OrderItemVO> records = result.getRecords().stream()
                .map(this::toOrderItemVO)
                .filter(item -> keyword == null || keyword.isBlank() || item.getTaskTitle().contains(keyword))
                .toList();
        return PageResult.of(result.getTotal(), page, size, records);
    }

    public OrderDetailVO getOrder(Long orderId) {
        refreshTimedOutOrders();
        Order order = requireOrder(orderId);
        ensureParticipant(order);
        return toOrderDetailVO(order);
    }

    @Transactional
    public void completeOrder(Long orderId, OrderCompleteRequest request) {
        refreshTimedOutOrders();
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(order.getServiceProviderId(), currentUserId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
        }
        if (!OrderStatus.IN_PROGRESS.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (order.getCancelReason() != null && !order.getCancelReason().isBlank()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单有未处理的取消申请，不能提交完成");
        }
        if (request.getProofImageId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "必须上传完成凭证后才能提交完成");
        }

        FileRecord proof = fileService.requireOwnedFile(request.getProofImageId(), UploadBusinessType.ORDER_PROOF);
        order.setCompletionProofUrl(proof.getFileUrl());
        order.setStatus(OrderStatus.PENDING_COMPLETION);
        orderMapper.updateById(order);
        saveStatusLog(
                order.getId(),
                OrderStatus.IN_PROGRESS,
                OrderStatus.PENDING_COMPLETION,
                currentUserId,
                request.getNote() != null && !request.getNote().isBlank() ? request.getNote() : "服务方提交完成"
        );
        notificationService.createOrderStatusNotification(order.getPublisherId(), order.getId(), OrderStatus.PENDING_COMPLETION);
        publishOrderChange(order);
    }

    @Transactional
    public void confirmCompletion(Long orderId) {
        refreshTimedOutOrders();
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(order.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
        }
        if (!OrderStatus.PENDING_COMPLETION.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderMapper.updateById(order);
        Task task = requireTask(order.getTaskId());
        task.setStatus(TaskStatus.COMPLETED);
        taskMapper.updateById(task);
        saveStatusLog(order.getId(), OrderStatus.PENDING_COMPLETION, OrderStatus.COMPLETED, currentUserId, "发布方确认完成");

        notificationService.createOrderStatusNotification(order.getServiceProviderId(), order.getId(), OrderStatus.COMPLETED);
        String taskTitle = task.getTitle();
        notificationService.createReviewRequestNotification(order.getPublisherId(), order.getId(), taskTitle);
        notificationService.createReviewRequestNotification(order.getServiceProviderId(), order.getId(), taskTitle);
        publishOrderChange(order);
        realtimeEventPublisher.broadcast(RealtimeEventPublisher.TASKS_CHANGED, task.getId());
    }

    @Transactional
    public void cancelOrder(Long orderId, OrderCancelRequest request) {
        refreshTimedOutOrders();
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        ensureParticipant(order);
        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }
        if (OrderStatus.TIMEOUT.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单已超时，不能继续操作");
        }
        if (OrderStatus.COMPLETED.equals(order.getStatus()) || OrderStatus.REVIEWED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_COMPLETED);
        }
        if (OrderStatus.PENDING_CONFIRM.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单已退回待接单状态，不能重复取消");
        }

        OrderStatus fromStatus = order.getStatus();
        if (Objects.equals(currentUserId, order.getPublisherId())) {
            Long previousServiceProviderId = order.getServiceProviderId();
            String reason = request.getReason().trim();
            order.setCancelReason(null);
            order.setStatus(OrderStatus.PENDING_CONFIRM);
            order.setServiceProviderId(null);
            orderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Order>()
                    .eq("id", order.getId())
                    .set("status", OrderStatus.PENDING_CONFIRM.name())
                    .set("cancel_reason", null)
                    .set("service_provider_id", null));

            Task task = requireTask(order.getTaskId());
            task.setStatus(TaskStatus.OPEN);
            taskMapper.updateById(task);
            applicationMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Application>()
                    .eq("task_id", task.getId())
                    .eq("status", ApplicationStatus.APPROVED.name())
                    .set("status", ApplicationStatus.CANCELLED.name()));

            saveStatusLog(order.getId(), fromStatus, OrderStatus.PENDING_CONFIRM, currentUserId, reason);

            notificationService.createOrderStatusNotification(previousServiceProviderId, order.getId(), OrderStatus.PENDING_CONFIRM);
            realtimeEventPublisher.user(order.getPublisherId(), RealtimeEventPublisher.ORDERS_CHANGED, order.getId());
            realtimeEventPublisher.user(previousServiceProviderId, RealtimeEventPublisher.ORDERS_CHANGED, order.getId());
            realtimeEventPublisher.broadcast(RealtimeEventPublisher.TASKS_CHANGED, task.getId());
            return;
        }

        if (order.getCancelReason() != null && !order.getCancelReason().isBlank()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "已提交取消申请，请等待发布方处理");
        }
        Long cancelRequestCount = orderStatusLogMapper.selectCount(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, order.getId())
                .eq(OrderStatusLog::getOperatorId, currentUserId)
                .likeRight(OrderStatusLog::getReason, "服务方申请取消："));
        if (cancelRequestCount != null && cancelRequestCount >= 2) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "取消申请最多只能提交两次");
        }

        order.setCancelReason(request.getReason().trim());
        order.setStatus(OrderStatus.IN_PROGRESS);
        orderMapper.updateById(order);
        saveStatusLog(order.getId(), fromStatus, OrderStatus.IN_PROGRESS, currentUserId, "服务方申请取消：" + request.getReason().trim());
        notificationService.createOrderActionNotification(order.getPublisherId(), order.getId(), "服务方申请取消服务", "服务方申请取消订单，原因：" + request.getReason().trim());
        publishOrderChange(order);
    }

    @Transactional
    public void approveCancelRequest(Long orderId) {
        refreshTimedOutOrders();
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(order.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
        }
        ensurePendingCancelRequest(order);

        String reason = order.getCancelReason();
        Long previousServiceProviderId = order.getServiceProviderId();
        order.setStatus(OrderStatus.PENDING_CONFIRM);
        order.setCancelReason(null);
        order.setServiceProviderId(null);
        orderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Order>()
                .eq("id", order.getId())
                .set("status", OrderStatus.PENDING_CONFIRM.name())
                .set("cancel_reason", null)
                .set("service_provider_id", null));

        Task task = requireTask(order.getTaskId());
        task.setStatus(TaskStatus.OPEN);
        taskMapper.updateById(task);
        applicationMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Application>()
                .eq("task_id", task.getId())
                .eq("status", ApplicationStatus.APPROVED.name())
                .set("status", ApplicationStatus.CANCELLED.name()));

        saveStatusLog(order.getId(), OrderStatus.IN_PROGRESS, OrderStatus.PENDING_CONFIRM, currentUserId, "发布方同意取消申请：" + reason);
        notificationService.createOrderActionNotification(previousServiceProviderId, order.getId(), "发布方已同意取消申请", "订单已退回待接单状态");
        realtimeEventPublisher.user(order.getPublisherId(), RealtimeEventPublisher.ORDERS_CHANGED, order.getId());
        realtimeEventPublisher.user(previousServiceProviderId, RealtimeEventPublisher.ORDERS_CHANGED, order.getId());
        realtimeEventPublisher.broadcast(RealtimeEventPublisher.TASKS_CHANGED, task.getId());
    }

    @Transactional
    public void rejectCancelRequest(Long orderId) {
        refreshTimedOutOrders();
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(order.getPublisherId(), currentUserId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
        }
        ensurePendingCancelRequest(order);

        String reason = order.getCancelReason();
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setCancelReason(null);
        orderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Order>()
                .eq("id", order.getId())
                .set("status", OrderStatus.IN_PROGRESS.name())
                .set("cancel_reason", null));
        saveStatusLog(order.getId(), OrderStatus.IN_PROGRESS, OrderStatus.IN_PROGRESS, currentUserId, "发布方拒绝取消申请：" + reason);
        notificationService.createOrderActionNotification(order.getServiceProviderId(), order.getId(), "发布方已拒绝取消申请", "发布方已拒绝取消申请，订单继续进行");
        publishOrderChange(order);
    }

    @Transactional
    public void submitReview(Long orderId, ReviewCreateRequest request) {
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        ensureParticipant(order);
        if (!OrderStatus.COMPLETED.equals(order.getStatus()) && !OrderStatus.REVIEWED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.REVIEW_ORDER_NOT_COMPLETED);
        }

        long existing = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
                .eq(Review::getReviewerId, currentUserId));
        if (existing > 0) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = new Review();
        review.setOrderId(orderId);
        review.setReviewerId(currentUserId);
        review.setRevieweeId(Objects.equals(currentUserId, order.getPublisherId()) ? order.getServiceProviderId() : order.getPublisherId());
        review.setRating(request.getRating());
        review.setContent(request.getContent().trim());
        reviewMapper.insert(review);

        int creditChange = (request.getRating() - 3) * 5;
        CreditLog latest = creditLogMapper.selectOne(new LambdaQueryWrapper<CreditLog>()
                .eq(CreditLog::getUserId, review.getRevieweeId())
                .orderByDesc(CreditLog::getId)
                .last("LIMIT 1"));
        int scoreBefore = clampCreditScore(latest != null ? latest.getScoreAfter() : DEFAULT_CREDIT_SCORE);
        int scoreAfter = clampCreditScore(scoreBefore + creditChange);
        CreditLog creditLog = new CreditLog();
        creditLog.setUserId(review.getRevieweeId());
        creditLog.setChangeAmount(scoreAfter - scoreBefore);
        creditLog.setScoreBefore(scoreBefore);
        creditLog.setScoreAfter(scoreAfter);
        creditLog.setReason("订单 #" + orderId + " 评价评分：" + request.getRating());
        creditLog.setRelatedOrderId(orderId);
        creditLogMapper.insert(creditLog);

        long reviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, orderId));
        if (reviewCount >= 2 && !OrderStatus.REVIEWED.equals(order.getStatus())) {
            order.setStatus(OrderStatus.REVIEWED);
            orderMapper.updateById(order);
            saveStatusLog(order.getId(), OrderStatus.COMPLETED, OrderStatus.REVIEWED, currentUserId, "双方已完成评价");
        }
        publishOrderChange(order);
        realtimeEventPublisher.user(review.getRevieweeId(), RealtimeEventPublisher.PROFILE_CHANGED, review.getRevieweeId());
    }

    private void publishOrderChange(Order order) {
        realtimeEventPublisher.user(order.getPublisherId(), RealtimeEventPublisher.ORDERS_CHANGED, order.getId());
        realtimeEventPublisher.user(order.getServiceProviderId(), RealtimeEventPublisher.ORDERS_CHANGED, order.getId());
    }

    public List<ReviewItemVO> listReviews(Long orderId) {
        Order order = requireOrder(orderId);
        ensureParticipant(order);
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .orderByDesc(Review::getCreatedAt))
                .stream()
                .map(this::toReviewItemVO)
                .toList();
    }

    public List<OrderStatusLogVO> listStatusLogs(Long orderId) {
        Order order = requireOrder(orderId);
        ensureParticipant(order);
        return orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, orderId)
                        .orderByAsc(OrderStatusLog::getCreatedAt))
                .stream()
                .map(this::toStatusLogVO)
                .toList();
    }

    private OrderDetailVO toOrderDetailVO(Order order) {
        Task task = requireTask(order.getTaskId());
        List<OrderStatusLog> statusLogs = orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, order.getId())
                .orderByAsc(OrderStatusLog::getCreatedAt));
        OrderDetailVO vo = new OrderDetailVO();
        copyBaseOrderFields(order, task, vo);
        vo.setTaskDescription(task.getDescription());
        vo.setCampus(task.getCampus());
        vo.setRewardType(task.getRewardType());
        vo.setRewardAmount(task.getRewardAmount());
        vo.setPaymentMethod(task.getPaymentMethod());
        vo.setProofImageUrl(order.getCompletionProofUrl());
        vo.setCompletionNote(findCompletionNote(statusLogs));
        vo.setTaskImageUrls(taskImageMapper.selectList(new LambdaQueryWrapper<TaskImage>()
                        .eq(TaskImage::getTaskId, order.getTaskId())
                        .orderByAsc(TaskImage::getSortOrder))
                .stream().map(TaskImage::getImageUrl).toList());
        vo.setStatusLogs(statusLogs.stream()
                .map(this::toStatusLogVO)
                .toList());
        vo.setTaskFiles(taskFileMapper.selectList(new LambdaQueryWrapper<TaskFile>()
                        .eq(TaskFile::getTaskId, order.getTaskId())
                        .orderByAsc(TaskFile::getSortOrder))
                .stream()
                .map(item -> {
                    FileRecord file = fileService.requireFile(item.getFileRecordId());
                    return new com.campushub.vo.task.TaskFileVO(item.getId(), file.getFileName(), file.getFileSize());
                })
                .toList());
        vo.setTaskFileDownloadAllowed(Objects.equals(SecurityUtils.getCurrentUserId().orElse(null), order.getServiceProviderId()));
        return vo;
    }

    private OrderItemVO toOrderItemVO(Order order) {
        Task task = requireTask(order.getTaskId());
        OrderItemVO vo = new OrderItemVO();
        copyBaseOrderFields(order, task, vo);
        return vo;
    }

    private void copyBaseOrderFields(Order order, Task task, OrderItemVO vo) {
        vo.setId(order.getId());
        vo.setTaskId(order.getTaskId());
        vo.setTaskTitle(task.getTitle());
        vo.setPublisherId(order.getPublisherId());
        vo.setPublisherNickname(findNickname(order.getPublisherId()));
        vo.setPublisherAvatarUrl(findAvatarUrl(order.getPublisherId()));
        if (OrderStatus.PENDING_CONFIRM.equals(order.getStatus())) {
            vo.setServiceProviderId(null);
            vo.setServiceProviderNickname("暂无服务方");
        } else {
            vo.setServiceProviderId(order.getServiceProviderId());
            vo.setServiceProviderNickname(findNickname(order.getServiceProviderId()));
            vo.setServiceProviderAvatarUrl(findAvatarUrl(order.getServiceProviderId()));
        }
        vo.setStatus(order.getStatus());
        vo.setCancelReason(order.getCancelReason());
        vo.setCreatedAt(order.getCreatedAt());
        List<String> imageUrls = taskImageMapper.selectList(new LambdaQueryWrapper<TaskImage>()
                        .eq(TaskImage::getTaskId, task.getId())
                        .orderByAsc(TaskImage::getSortOrder))
                .stream()
                .map(TaskImage::getImageUrl)
                .toList();
        if (!imageUrls.isEmpty()) {
            vo.setTaskImageUrl(imageUrls.get(0));
        }
    }

    private OrderStatusLogVO toStatusLogVO(OrderStatusLog log) {
        OrderStatus fromStatus = log.getFromStatus() == null ? null : OrderStatus.valueOf(log.getFromStatus());
        OrderStatus toStatus = log.getToStatus() == null ? null : OrderStatus.valueOf(log.getToStatus());
        return new OrderStatusLogVO(log.getId(), fromStatus, toStatus, log.getOperatorId(), findNickname(log.getOperatorId()), log.getReason(), log.getCreatedAt());
    }

    private ReviewItemVO toReviewItemVO(Review review) {
        return new ReviewItemVO(
                review.getId(),
                review.getOrderId(),
                review.getReviewerId(),
                findNickname(review.getReviewerId()),
                review.getRevieweeId(),
                findNickname(review.getRevieweeId()),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }

    private void saveStatusLog(Long orderId, OrderStatus fromStatus, OrderStatus toStatus, Long operatorId, String reason) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus != null ? fromStatus.name() : null);
        log.setToStatus(toStatus.name());
        log.setOperatorId(operatorId);
        log.setReason(reason);
        orderStatusLogMapper.insert(log);
    }

    private int clampCreditScore(int score) {
        return Math.max(MIN_CREDIT_SCORE, Math.min(MAX_CREDIT_SCORE, score));
    }

    private String findCompletionNote(List<OrderStatusLog> statusLogs) {
        return statusLogs.stream()
                .filter(log -> OrderStatus.PENDING_COMPLETION.name().equals(log.getToStatus()))
                .map(OrderStatusLog::getReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private void refreshTimedOutOrders() {
        taskMapper.expireOpenTasksPastDeadline();
        orderMapper.timeoutPendingConfirmOrdersForExpiredTasks();
        orderMapper.timeoutInProgressOrdersPastTaskDeadline();
        taskMapper.expireInProgressTasksWithTimedOutOrders();
    }

    private void ensureParticipant(Order order) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (OrderStatus.PENDING_CONFIRM.equals(order.getStatus())) {
            if (!Objects.equals(currentUserId, order.getPublisherId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
            }
            return;
        }
        if (!Objects.equals(currentUserId, order.getPublisherId()) && !Objects.equals(currentUserId, order.getServiceProviderId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
        }
    }

    private void ensurePendingCancelRequest(Order order) {
        if (!OrderStatus.IN_PROGRESS.equals(order.getStatus()) || order.getCancelReason() == null || order.getCancelReason().isBlank()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private Task requireTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    private String findNickname(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        return profile != null ? profile.getNickname() : "CampusHub 用户";
    }

    private String findAvatarUrl(Long userId) {
        if (userId == null) return null;
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        return profile != null ? profile.getAvatarUrl() : null;
    }
}
