package com.campushub.realtime;

import com.campushub.security.JwtTokenProvider;
import com.campushub.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String protocols = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        String token = protocols == null
                ? null
                : Arrays.stream(protocols.split(","))
                        .map(String::trim)
                        .filter(protocol -> !"campushub".equals(protocol))
                        .findFirst()
                        .orElse(null);
        if (token == null || token.isBlank()) {
            return true;
        }
        if (tokenBlacklistService.isBlacklisted(token)
                || !jwtTokenProvider.validateToken(token)) {
            return false;
        }
        attributes.put("userId", jwtTokenProvider.getUserIdFromToken(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
