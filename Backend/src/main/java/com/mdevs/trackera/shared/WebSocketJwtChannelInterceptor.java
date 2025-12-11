package com.mdevs.trackera.shared;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.service.UserService;
import com.mdevs.trackera.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;

    private final UserService userService;

    public WebSocketJwtChannelInterceptor(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                String token = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
                if (token == null) throw new SecurityException("Missing token");
                token = token.replace("Bearer ", "");
                Claims claims = jwtUtil.validateAndGetTokenPayload(token, true);
                User loggedUser = userService.findByUuidOrThrow(claims.get("id", String.class));
                accessor.getSessionAttributes().put("user", loggedUser);
            } catch (SecurityException e) {
                throw new MessageDeliveryException(message, "401:Unauthorized", e);
            }
        }
        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return;
        }

        String sessionId = accessor.getSessionId();
        String user = accessor.getSessionAttributes() != null ? ((User) (accessor.getSessionAttributes().get("user"))).getId().toString() : "anonymous";

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("WebSocket connected | sessionId={} | user={}", sessionId, user);
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            log.info("WebSocket disconnected | sessionId={} | user={}", sessionId, user);
        }

        ChannelInterceptor.super.postSend(message, channel, sent);
    }
}
