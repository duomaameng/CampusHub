package com.campushub.config;

import com.campushub.realtime.RealtimeHandshakeInterceptor;
import com.campushub.realtime.RealtimeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler webSocketHandler;
    private final RealtimeHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/api/ws/updates")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
