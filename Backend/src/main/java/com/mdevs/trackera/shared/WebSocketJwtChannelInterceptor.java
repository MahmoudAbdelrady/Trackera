package com.mdevs.trackera.shared;

import com.mdevs.trackera.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;

    public WebSocketJwtChannelInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token == null) throw new SecurityException("Missing token");
            token = token.replace("Bearer ", "");
            Claims claims = jwtUtil.validateAndGetTokenPayload(token, true);
            accessor.getSessionAttributes().put("userId", claims.get("id"));
        }
        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        log.info("WebSocket connection completed.");
        ChannelInterceptor.super.postSend(message, channel, sent);
    }
}
