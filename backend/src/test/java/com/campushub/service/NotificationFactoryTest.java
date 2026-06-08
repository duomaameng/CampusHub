package com.campushub.service;

import com.campushub.entity.Notification;
import com.campushub.enums.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationFactoryTest {

    private final NotificationFactory factory = new NotificationFactory();

    @Test
    void shouldCreateOrderActionNotificationForCancelRequestRegression() {
        Notification notification = factory.orderAction(10001L, 7002L, "服务方申请取消服务", "服务方申请取消订单");

        assertThat(notification.getReceiverId()).isEqualTo(10001L);
        assertThat(notification.getRelatedOrderId()).isEqualTo(7002L);
        assertThat(notification.getType()).isEqualTo(NotificationType.ORDER_STATUS);
        assertThat(notification.getIsRead()).isFalse();
        assertThat(notification.getIsDeleted()).isFalse();
        assertThat(notification.getTitle()).isEqualTo("服务方申请取消服务");
    }
}
