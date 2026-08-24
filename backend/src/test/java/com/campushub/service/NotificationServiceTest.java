package com.campushub.service;

import com.campushub.entity.Notification;
import com.campushub.mapper.NotificationMapper;
import com.campushub.realtime.RealtimeEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationMapper notificationMapper;
    @Mock private RealtimeEventPublisher realtimeEventPublisher;
    private final NotificationFactory notificationFactory = new NotificationFactory();

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldPersistOrderActionNotificationForCancelDecision() {
        notificationService = new NotificationService(notificationMapper, notificationFactory, realtimeEventPublisher);

        notificationService.createOrderActionNotification(10002L, 7002L, "发布方已同意取消申请", "订单已退回待接单状态");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertThat(captor.getValue().getReceiverId()).isEqualTo(10002L);
        assertThat(captor.getValue().getRelatedOrderId()).isEqualTo(7002L);
        assertThat(captor.getValue().getContent()).isEqualTo("订单已退回待接单状态");
    }
}
