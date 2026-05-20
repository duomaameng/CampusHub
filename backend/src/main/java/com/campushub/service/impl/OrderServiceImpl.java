package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.dto.response.OrderDetailResponse;
import com.campushub.dto.response.OrderItemResponse;
import com.campushub.entity.*;
import com.campushub.enums.OrderStatus;
import com.campushub.mapper.*;
import com.campushub.security.SecurityUtils;
import com.campushub.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final OrderMessageMapper orderMessageMapper;
    private final TaskMapper taskMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public PageResult<OrderItemResponse> listOrders(String role, String status, String keyword,
                                                     int page, int size) {
        Long userId = SecurityUtils.requireCurrentUserId();

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if ("PUBLISHER".equals(role)) {
            wrapper.eq(Order::getPublisherId, userId);
        } else if ("PROVIDER".equals(role)) {
            wrapper.eq(Order::getServiceProviderId, userId);
        } else {
            wrapper.and(w -> w.eq(Order::getPublisherId, userId).or().eq(Order::getServiceProviderId, userId));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, OrderStatus.valueOf(status));
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        IPage<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);

        List<OrderItemResponse> records = orderPage.getRecords().stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());

        if (keyword != null && !keyword.isEmpty()) {
            records = records.stream()
                    .filter(r -> r.getTaskTitle().contains(keyword))
                    .collect(Collectors.toList());
        }

        return PageResult.of(orderPage.getTotal(), page, size, records);
    }

    @Override
    public OrderDetailResponse getOrder(Long orderId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getPublisherId().equals(userId) && !order.getServiceProviderId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
        }

        return toDetail(order);
    }

    @Override
    @Transactional
    public void submitCompletion(Long orderId, Long proofImageId, String note) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Order order = getAndValidate(orderId, userId, OrderStatus.IN_PROGRESS);
        if (!order.getServiceProviderId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT, "只有服务方可以提交完成凭证");
        }

        String fromStatus = order.getStatus().getValue();
        order.setStatus(OrderStatus.PENDING_COMPLETION);
        if (proofImageId != null) {
            order.setCompletionProofUrl("/uploads/proofs/" + proofImageId);
        }
        orderMapper.updateById(order);

        logStatus(orderId, fromStatus, order.getStatus().getValue(), userId, note);
    }

    @Override
    @Transactional
    public void confirmCompletion(Long orderId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Order order = getAndValidate(orderId, userId, OrderStatus.PENDING_COMPLETION);
        if (!order.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT, "只有发布者可以确认完成");
        }

        String fromStatus = order.getStatus().getValue();
        order.setStatus(OrderStatus.COMPLETED);
        orderMapper.updateById(order);

        logStatus(orderId, fromStatus, order.getStatus().getValue(), userId, "发布者确认完成");
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getPublisherId().equals(userId) && !order.getServiceProviderId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
        }

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.CANCELLED
                || currentStatus == OrderStatus.REVIEWED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        String fromStatus = currentStatus.getValue();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        logStatus(orderId, fromStatus, order.getStatus().getValue(), userId, reason);
    }

    @Override
    @Transactional
    public void disputeOrder(Long orderId, String reason) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Order order = getAndValidate(orderId, userId, OrderStatus.PENDING_COMPLETION);
        if (!order.getPublisherId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT, "只有发布者可以发起争议");
        }

        String fromStatus = order.getStatus().getValue();
        order.setStatus(OrderStatus.DISPUTE);
        orderMapper.updateById(order);

        logStatus(orderId, fromStatus, order.getStatus().getValue(), userId, reason);
    }

    private Order getAndValidate(Long orderId, Long userId, OrderStatus expectedStatus) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getPublisherId().equals(userId) && !order.getServiceProviderId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PARTICIPANT);
        }
        if (order.getStatus() != expectedStatus) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        return order;
    }

    private void logStatus(Long orderId, String fromStatus, String toStatus, Long operatorId, String reason) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setReason(reason);
        orderStatusLogMapper.insert(log);
    }

    private OrderItemResponse toOrderItem(Order order) {
        Task task = taskMapper.selectById(order.getTaskId());
        String publisherNickname = getNickname(order.getPublisherId());
        String providerNickname = getNickname(order.getServiceProviderId());

        return OrderItemResponse.builder()
                .id(order.getId())
                .taskId(order.getTaskId())
                .taskTitle(task != null ? task.getTitle() : "")
                .taskDescription(task != null ? task.getDescription() : "")
                .campus(task != null ? task.getCampus() : "")
                .rewardType(task != null ? task.getRewardType() : null)
                .publisherId(order.getPublisherId())
                .publisherNickname(publisherNickname)
                .serviceProviderId(order.getServiceProviderId())
                .serviceProviderNickname(providerNickname)
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderDetailResponse toDetail(Order order) {
        Task task = taskMapper.selectById(order.getTaskId());
        String publisherNickname = getNickname(order.getPublisherId());
        String providerNickname = getNickname(order.getServiceProviderId());

        List<OrderStatusLog> statusLogs = orderStatusLogMapper.selectList(
                new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, order.getId())
                        .orderByAsc(OrderStatusLog::getCreatedAt));

        List<OrderMessage> messages = orderMessageMapper.selectList(
                new LambdaQueryWrapper<OrderMessage>()
                        .eq(OrderMessage::getOrderId, order.getId())
                        .orderByAsc(OrderMessage::getCreatedAt));

        return OrderDetailResponse.builder()
                .id(order.getId())
                .taskId(order.getTaskId())
                .taskTitle(task != null ? task.getTitle() : "")
                .taskDescription(task != null ? task.getDescription() : "")
                .campus(task != null ? task.getCampus() : "")
                .rewardType(task != null ? task.getRewardType() : null)
                .publisherId(order.getPublisherId())
                .publisherNickname(publisherNickname)
                .serviceProviderId(order.getServiceProviderId())
                .serviceProviderNickname(providerNickname)
                .status(order.getStatus())
                .statusLogs(statusLogs.stream().map(log ->
                        OrderDetailResponse.StatusLogItem.builder()
                                .id(log.getId())
                                .fromStatus(log.getFromStatus())
                                .toStatus(log.getToStatus())
                                .operatorNickname(getNickname(log.getOperatorId()))
                                .reason(log.getReason())
                                .createdAt(log.getCreatedAt())
                                .build()
                ).collect(Collectors.toList()))
                .messages(messages.stream().map(msg ->
                        OrderDetailResponse.MessageItem.builder()
                                .id(msg.getId())
                                .senderId(msg.getSenderId())
                                .senderNickname(getNickname(msg.getSenderId()))
                                .messageType(msg.getMessageType())
                                .content(msg.getContent())
                                .imageUrl(msg.getImageUrl())
                                .createdAt(msg.getCreatedAt())
                                .build()
                ).collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private String getNickname(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        return profile != null ? profile.getNickname() : "未知用户";
    }
}
