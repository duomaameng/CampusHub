package com.campushub.realtime;

public record RealtimeChangeEvent(Long userId, RealtimeEvent event) {

    public static RealtimeChangeEvent broadcast(String type, Long entityId) {
        return new RealtimeChangeEvent(null, new RealtimeEvent(type, entityId));
    }

    public static RealtimeChangeEvent user(Long userId, String type, Long entityId) {
        return new RealtimeChangeEvent(userId, new RealtimeEvent(type, entityId));
    }
}
