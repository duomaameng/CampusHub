package com.campushub.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {

    private static final String USER_ID_ATTRIBUTE = "userId";

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public List<String> getSubProtocols() {
        return List.of("campushub");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void broadcast(RealtimeEvent event) {
        send(event, null);
    }

    public void sendToUser(Long userId, RealtimeEvent event) {
        send(event, userId);
    }

    private void send(RealtimeEvent event, Long targetUserId) {
        try {
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(event));
            for (WebSocketSession session : sessions) {
                if (targetUserId == null || targetUserId.equals(session.getAttributes().get(USER_ID_ATTRIBUTE))) {
                    send(session, message);
                }
            }
        } catch (IOException exception) {
            log.warn("Failed to serialize realtime event {}", event.type(), exception);
        }
    }

    private void send(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            sessions.remove(session);
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException exception) {
            sessions.remove(session);
            log.debug("Failed to send realtime event to session {}", session.getId(), exception);
        }
    }
}
