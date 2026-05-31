package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.dto.order.OrderCancelRequest;
import com.campushub.dto.order.OrderCompleteRequest;
import com.campushub.dto.order.OrderMessageRequest;
import com.campushub.dto.order.ReviewCreateRequest;
import com.campushub.entity.*;
import com.campushub.enums.MessageType;
import com.campushub.enums.OrderStatus;
import com.campushub.enums.TaskStatus;
import com.campushub.enums.UploadBusinessType;
import com.campushub.mapper.*;
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

    private final OrderMapper orderMapper;
    private final TaskMapper taskMapper;
    private final UserProfileMapper userProfileMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final OrderMessageMapper orderMessageMapper;
    private final ReviewMapper reviewMapper;
    private final NotificationService notificationService;
    private final FileService fileService;

    public PageResult<OrderItemVO> listOrders(int page, int size, String role, OrderStatus status, String keyword) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Page<Order> pageQuery = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreatedAt);

        if ("PUBLISHER".equalsIgnoreCase(role)) {
            wrapper.eq(Order::getPublisherId, currentUserId);
        } else if ("PROVIDER".equalsIgnoreCase(role)) {
            wrapper.eq(Order::getServiceProviderId, currentUserId);
        } else {
            wrapper.and(w -> w.eq(Order::getPublisherId, currentUserId).or().eq(Order::getServiceProviderId, currentUserId));
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }

        Page<Order> result = orderMapper.selectPage(pageQuery, wrapper);
        List<OrderItemVO> records = result.getRecords().stream()
                .map(this::toOrderItemVO)
                .filter(item -> keyword == null || keyword.isBlank() || item.getTaskTitle().contains(keyword))
                .toList();
        return PageResult.of(result.getTotal(), page, size, records);
    }

    public OrderDetailVO getOrder(Long orderId) {
        Order order = requireOrder(orderId);
        ensureParticipant(order);
        return toOrderDetailVO(order);
    }

    @Transactional
    public void completeOrder(Long orderId, OrderCompleteRequest request) {
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(order.getServiceProviderId(), currentUserId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
        }
        if (!OrderStatus.IN_PROGRESS.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        if (request.getProofImageId() != null) {
            FileRecord proof = fileService.requireOwnedFile(request.getProofImageId(), UploadBusinessType.ORDER_PROOF);
            order.setCompletionProofUrl(proof.getFileUrl());
        }
        order.setStatus(OrderStatus.PENDING_COMPLETION);
        orderMapper.updateById(order);
        saveStatusLog(
                order.getId(),
                OrderStatus.IN_PROGRESS,
                OrderStatus.PENDING_COMPLETION,
                currentUserId,
                request.getNote() != null && !request.getNote().isBlank() ? request.getNote() : "Provider submitted completion"
        );
        notificationService.createOrderStatusNotification(order.getPublisherId(), order.getId(), OrderStatus.PENDING_COMPLETION);
    }

    @Transactional
    public void confirmCompletion(Long orderId) {
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
        saveStatusLog(order.getId(), OrderStatus.PENDING_COMPLETION, OrderStatus.COMPLETED, currentUserId, "Publisher confirmed completion");

        notificationService.createOrderStatusNotification(order.getServiceProviderId(), order.getId(), OrderStatus.COMPLETED);
        String taskTitle = task.getTitle();
        notificationService.createReviewRequestNotification(order.getPublisherId(), order.getId(), taskTitle);
        notificationService.createReviewRequestNotification(order.getServiceProviderId(), order.getId(), taskTitle);
    }

    @Transactional
    public void cancelOrder(Long orderId, OrderCancelRequest request) {
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        ensureParticipant(order);
        if (OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }
        if (OrderStatus.COMPLETED.equals(order.getStatus()) || OrderStatus.REVIEWED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_COMPLETED);
        }

        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request.getReason().trim());
        orderMapper.updateById(order);
        saveStatusLog(order.getId(), fromStatus, OrderStatus.CANCELLED, currentUserId, request.getReason().trim());

        Long receiverId = Objects.equals(currentUserId, order.getPublisherId()) ? order.getServiceProviderId() : order.getPublisherId();
        notificationService.createOrderStatusNotification(receiverId, order.getId(), OrderStatus.CANCELLED);
    }

    @Transactional
    public void sendMessage(Long orderId, OrderMessageRequest request) {
        Order order = requireOrder(orderId);
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        ensureParticipant(order);

        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setSenderId(currentUserId);
        message.setMessageType(request.getMessageType());
        message.setIsRead(false);

        if (MessageType.TEXT.equals(request.getMessageType())) {
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new BusinessException(ErrorCode.MESSAGE_EMPTY);
            }
            message.setContent(request.getContent().trim());
        } else if (MessageType.IMAGE.equals(request.getMessageType())) {
            FileRecord image = request.getImageId() == null ? null : fileService.requireOwnedFile(request.getImageId(), UploadBusinessType.CHAT_IMAGE);
            if (image == null) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Image file not found");
            }
            message.setImageUrl(image.getFileUrl());
        }

        orderMessageMapper.insert(message);

        Long receiverId = Objects.equals(currentUserId, order.getPublisherId())
                ? order.getServiceProviderId()
                : order.getPublisherId();
        String senderNickname = findNickname(currentUserId);
        String preview = MessageType.IMAGE.equals(request.getMessageType())
                ? "发送了一张图片"
                : message.getContent();
        notificationService.createOrderMessageNotification(receiverId, order.getId(), senderNickname, preview);
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

        long reviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, orderId));
        if (reviewCount >= 2 && !OrderStatus.REVIEWED.equals(order.getStatus())) {
            order.setStatus(OrderStatus.REVIEWED);
            orderMapper.updateById(order);
            saveStatusLog(order.getId(), OrderStatus.COMPLETED, OrderStatus.REVIEWED, currentUserId, "Both sides completed reviews");
        }
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

    public List<OrderMessageVO> listMessages(Long orderId) {
        Order order = requireOrder(orderId);
        ensureParticipant(order);
        return orderMessageMapper.selectList(new LambdaQueryWrapper<OrderMessage>()
                        .eq(OrderMessage::getOrderId, orderId)
                        .orderByAsc(OrderMessage::getCreatedAt))
                .stream()
                .map(this::toOrderMessageVO)
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
        vo.setProofImageUrl(order.getCompletionProofUrl());
        vo.setCompletionNote(findCompletionNote(statusLogs));
        vo.setStatusLogs(statusLogs.stream()
                .map(this::toStatusLogVO)
                .toList());
        vo.setMessages(orderMessageMapper.selectList(new LambdaQueryWrapper<OrderMessage>()
                        .eq(OrderMessage::getOrderId, order.getId())
                        .orderByAsc(OrderMessage::getCreatedAt))
                .stream()
                .map(this::toOrderMessageVO)
                .toList());
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
        vo.setServiceProviderId(order.getServiceProviderId());
        vo.setServiceProviderNickname(findNickname(order.getServiceProviderId()));
        vo.setStatus(order.getStatus());
        vo.setCreatedAt(order.getCreatedAt());
    }

    private OrderStatusLogVO toStatusLogVO(OrderStatusLog log) {
        OrderStatus fromStatus = log.getFromStatus() == null ? null : OrderStatus.valueOf(log.getFromStatus());
        OrderStatus toStatus = log.getToStatus() == null ? null : OrderStatus.valueOf(log.getToStatus());
        return new OrderStatusLogVO(log.getId(), fromStatus, toStatus, findNickname(log.getOperatorId()), log.getReason(), log.getCreatedAt());
    }

    private OrderMessageVO toOrderMessageVO(OrderMessage message) {
        return new OrderMessageVO(
                message.getId(),
                message.getOrderId(),
                message.getSenderId(),
                findNickname(message.getSenderId()),
                message.getMessageType(),
                message.getContent(),
                message.getImageUrl(),
                message.getCreatedAt()
        );
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

    private String findCompletionNote(List<OrderStatusLog> statusLogs) {
        return statusLogs.stream()
                .filter(log -> OrderStatus.PENDING_COMPLETION.name().equals(log.getToStatus()))
                .map(OrderStatusLog::getReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private void ensureParticipant(Order order) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (!Objects.equals(currentUserId, order.getPublisherId()) && !Objects.equals(currentUserId, order.getServiceProviderId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
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
        return profile != null ? profile.getNickname() : "CampusHub User";
    }
}
