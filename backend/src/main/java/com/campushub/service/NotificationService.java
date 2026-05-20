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
    }

    @Transactional
    public void createApplicationNotification(Long receiverId, Long taskId, String applicantNickname, String taskTitle) {
        notificationMapper.insert(notificationFactory.application(receiverId, taskId, applicantNickname, taskTitle));
    }

    @Transactional
    public void createOrderStatusNotification(Long receiverId, Long orderId, OrderStatus orderStatus) {
        notificationMapper.insert(notificationFactory.orderStatus(receiverId, orderId, orderStatus));
    }

    @Transactional
    public void createReviewRequestNotification(Long receiverId, Long orderId, String taskTitle) {
        notificationMapper.insert(notificationFactory.reviewRequest(receiverId, orderId, taskTitle));
    }

    @Transactional
    public void createReportResultNotification(Long receiverId, Long taskId, String resultSummary) {
        notificationMapper.insert(notificationFactory.reportResult(receiverId, taskId, resultSummary));
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
