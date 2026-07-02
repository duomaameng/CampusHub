package com.campushub.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    public static final String TASKS_CHANGED = "TASKS_CHANGED";
    public static final String APPLICATIONS_CHANGED = "APPLICATIONS_CHANGED";
    public static final String ORDERS_CHANGED = "ORDERS_CHANGED";
    public static final String MESSAGES_CHANGED = "MESSAGES_CHANGED";
    public static final String NOTIFICATIONS_CHANGED = "NOTIFICATIONS_CHANGED";
    public static final String ANNOUNCEMENTS_CHANGED = "ANNOUNCEMENTS_CHANGED";
    public static final String ADMIN_CHANGED = "ADMIN_CHANGED";
    public static final String PROFILE_CHANGED = "PROFILE_CHANGED";

    private final ApplicationEventPublisher publisher;

    public void broadcast(String type, Long entityId) {
        publisher.publishEvent(RealtimeChangeEvent.broadcast(type, entityId));
    }

    public void user(Long userId, String type, Long entityId) {
        if (userId != null) {
            publisher.publishEvent(RealtimeChangeEvent.user(userId, type, entityId));
        }
    }
}
