package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageResult;
import com.campushub.entity.Notification;
import com.campushub.enums.OrderStatus;
import com.campushub.mapper.NotificationMapper;
import com.campushub.realtime.RealtimeEventPublisher;
import com.campushub.security.SecurityUtils;
import com.campushub.vo.NotificationItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationFactory notificationFactory;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public PageResult<NotificationItemVO> listCurrentUserNotifications(int page, int size, Boolean read) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Page<Notification> pageQuery = new Page<>(page, size);
        Page<Notification> result = (Page<Notification>) notificationMapper.selectUserNotifications(pageQuery, currentUserId, read);
        List<NotificationItemVO> records = result.getRecords().stream()
                .map(this::toItemVO)
                .toList();
        return PageResult.of(result.getTotal(), page, size, records);
    }

    public long countUnread() {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        return notificationMapper.countUnread(currentUserId);
    }

    @Transactional
    public void markRead(Long notificationId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Notification notification = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getReceiverId, currentUserId)
                        .eq(Notification::getIsDeleted, false)
                        .last("LIMIT 1")
        );
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }
        notification.setIsRead(true);
        notificationMapper.updateById(notification);
        publishNotificationChange(currentUserId);
    }

    @Transactional
    public void markAllRead() {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        notificationMapper.update(
                null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getReceiverId, currentUserId)
                        .eq(Notification::getIsDeleted, false)
                        .eq(Notification::getIsRead, false)
                        .set(Notification::getIsRead, true)
        );
        publishNotificationChange(currentUserId);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        Notification notification = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getReceiverId, currentUserId)
                        .eq(Notification::getIsDeleted, false)
                        .last("LIMIT 1")
        );
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notification.setIsDeleted(true);
        notificationMapper.updateById(notification);
        publishNotificationChange(currentUserId);
    }

    @Transactional
    public void deleteReadNotifications() {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        notificationMapper.update(
                null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getReceiverId, currentUserId)
                        .eq(Notification::getIsDeleted, false)
                        .eq(Notification::getIsRead, true)
                        .set(Notification::getIsDeleted, true)
        );
        publishNotificationChange(currentUserId);
    }

    @Transactional
    public void createApplicationNotification(Long receiverId, Long taskId, String applicantNickname, String taskTitle) {
        notificationMapper.insert(notificationFactory.application(receiverId, taskId, applicantNickname, taskTitle));
        publishNotificationChange(receiverId);
        realtimeEventPublisher.user(receiverId, RealtimeEventPublisher.TASKS_CHANGED, taskId);
        realtimeEventPublisher.user(receiverId, RealtimeEventPublisher.APPLICATIONS_CHANGED, taskId);
        realtimeEventPublisher.broadcast(RealtimeEventPublisher.TASKS_CHANGED, taskId);
    }

    @Transactional
    public void createOrderStatusNotification(Long receiverId, Long orderId, OrderStatus orderStatus) {
        notificationMapper.insert(notificationFactory.orderStatus(receiverId, orderId, orderStatus));
        publishOrderNotification(receiverId, orderId);
    }

    @Transactional
    public void createOrderActionNotification(Long receiverId, Long orderId, String title, String content) {
        notificationMapper.insert(notificationFactory.orderAction(receiverId, orderId, title, content));
        publishOrderNotification(receiverId, orderId);
    }

    @Transactional
    public void createOrderMessageNotification(Long receiverId, Long orderId, String senderNickname, String preview) {
        notificationMapper.insert(notificationFactory.orderMessage(receiverId, orderId, senderNickname, preview));
        publishNotificationChange(receiverId);
        realtimeEventPublisher.user(receiverId, RealtimeEventPublisher.MESSAGES_CHANGED, orderId);
    }

    @Transactional
    public void createReviewRequestNotification(Long receiverId, Long orderId, String taskTitle) {
        notificationMapper.insert(notificationFactory.reviewRequest(receiverId, orderId, taskTitle));
        publishOrderNotification(receiverId, orderId);
    }

    @Transactional
    public void createReportResultNotification(Long receiverId, Long taskId, String resultSummary) {
        notificationMapper.insert(notificationFactory.reportResult(receiverId, taskId, resultSummary));
        publishNotificationChange(receiverId);
        realtimeEventPublisher.user(receiverId, RealtimeEventPublisher.TASKS_CHANGED, taskId);
    }

    @Transactional
    public void createReportResultOrderNotification(Long receiverId, Long orderId, String resultSummary) {
        notificationMapper.insert(notificationFactory.reportResultForOrder(receiverId, orderId, resultSummary));
        publishOrderNotification(receiverId, orderId);
    }

    private void publishOrderNotification(Long receiverId, Long orderId) {
        publishNotificationChange(receiverId);
        realtimeEventPublisher.user(receiverId, RealtimeEventPublisher.ORDERS_CHANGED, orderId);
    }

    private void publishNotificationChange(Long receiverId) {
        realtimeEventPublisher.user(receiverId, RealtimeEventPublisher.NOTIFICATIONS_CHANGED, null);
    }

    private NotificationItemVO toItemVO(Notification notification) {
        String targetType = notification.getRelatedOrderId() != null ? "ORDER" : "TASK";
        Long targetId = notification.getRelatedOrderId() != null ? notification.getRelatedOrderId() : notification.getRelatedTaskId();
        return new NotificationItemVO(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                targetType,
                targetId,
                Boolean.TRUE.equals(notification.getIsRead()),
                notification.getCreatedAt()
        );
    }
}
