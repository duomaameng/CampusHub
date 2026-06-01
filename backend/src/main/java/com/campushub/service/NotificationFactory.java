package com.campushub.service;

import com.campushub.entity.Notification;
import com.campushub.enums.NotificationType;
import com.campushub.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {

    public Notification application(Long receiverId, Long taskId, String applicantNickname, String taskTitle) {
        Notification notification = base(receiverId, NotificationType.APPLICATION);
        notification.setTitle("收到新的接单申请");
        notification.setContent(applicantNickname + " 申请接单：" + taskTitle);
        notification.setRelatedTaskId(taskId);
        return notification;
    }

    public Notification orderStatus(Long receiverId, Long orderId, OrderStatus orderStatus) {
        Notification notification = base(receiverId, NotificationType.ORDER_STATUS);
        notification.setTitle("订单状态已更新");
        notification.setContent("订单状态变更为：" + toOrderStatusText(orderStatus));
        notification.setRelatedOrderId(orderId);
        return notification;
    }

    public Notification orderAction(Long receiverId, Long orderId, String title, String content) {
        Notification notification = base(receiverId, NotificationType.ORDER_STATUS);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedOrderId(orderId);
        return notification;
    }

    public Notification orderMessage(Long receiverId, Long orderId, String senderNickname, String preview) {
        Notification notification = base(receiverId, NotificationType.ORDER_MESSAGE);
        notification.setTitle("订单收到新留言");
        notification.setContent(senderNickname + "：" + preview);
        notification.setRelatedOrderId(orderId);
        return notification;
    }

    public Notification reviewRequest(Long receiverId, Long orderId, String taskTitle) {
        Notification notification = base(receiverId, NotificationType.REVIEW_REQUEST);
        notification.setTitle("请完成本次评价");
        notification.setContent("订单已完成，请为任务“" + taskTitle + "”提交评价。");
        notification.setRelatedOrderId(orderId);
        return notification;
    }

    public Notification reportResult(Long receiverId, Long taskId, String resultSummary) {
        Notification notification = base(receiverId, NotificationType.REPORT_RESULT);
        notification.setTitle("举报处理结果已更新");
        notification.setContent(resultSummary);
        notification.setRelatedTaskId(taskId);
        return notification;
    }

    private Notification base(Long receiverId, NotificationType type) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setIsDeleted(false);
        return notification;
    }

    private String toOrderStatusText(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case PENDING_CONFIRM -> "待确认";
            case IN_PROGRESS -> "进行中";
            case PENDING_COMPLETION -> "待确认完成";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case DISPUTE -> "争议处理中";
            case REVIEWED -> "已评价";
        };
    }
}
