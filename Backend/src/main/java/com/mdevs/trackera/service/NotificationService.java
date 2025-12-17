package com.mdevs.trackera.service;

import com.mdevs.trackera.shared.SseRegistry;
import com.mdevs.trackera.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {
    private final SseRegistry sseRegistry;

    private final JwtUtil jwtUtil;

    public NotificationService(SseRegistry sseRegistry, JwtUtil jwtUtil) {
        this.sseRegistry = sseRegistry;
        this.jwtUtil = jwtUtil;
    }

    public SseEmitter createSubscription(String accessToken) {
        SseEmitter sseEmitter = new SseEmitter(0L);

        try {
            Claims claims = jwtUtil.getTokenPayload(accessToken, true);
            String userUuid = claims.get("id", String.class);

            sseRegistry.add(userUuid, sseEmitter);
            sseEmitter.onCompletion(() -> sseRegistry.remove(userUuid, sseEmitter));
            sseEmitter.onError((_) -> sseRegistry.remove(userUuid, sseEmitter));
        } catch (SecurityException e) {
            try {
                sseEmitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("status", 401, "error", e.getMessage())));
            } catch (IOException ex) {
                log.error("Error while sending SSE error event", ex);
            }
            sseEmitter.complete();
        }

        return sseEmitter;
    }

    public void sendNotification(String userUuid, String eventName, Object data) {
        for (SseEmitter emitter : sseRegistry.get(userUuid)) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.error("Error while sending SSE event to user: {}", userUuid, e);
                sseRegistry.remove(userUuid, emitter);
            }
        }
    }
}
