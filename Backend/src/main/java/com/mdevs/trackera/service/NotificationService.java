package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.notification.NotificationDTO;
import com.mdevs.trackera.shared.SseRegistry;
import com.mdevs.trackera.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SseRegistry sseRegistry;

    private final JwtUtil jwtUtil;

    private final ApplicationEventPublisher applicationEventPublisher;

    public SseEmitter createSubscription(String accessToken) {
        SseEmitter sseEmitter = new SseEmitter(0L);

        Claims claims;
        try {
            claims = jwtUtil.getTokenPayload(accessToken, true);
        } catch (SecurityException e) {
            return handleSseSecurityError(e, sseEmitter);
        }

        String userUuid = claims.get("id", String.class);
        sseRegistry.add(userUuid, sseEmitter);
        sseEmitter.onCompletion(() -> sseRegistry.remove(userUuid, sseEmitter));
        sseEmitter.onError((_) -> sseRegistry.remove(userUuid, sseEmitter));

        try {
            sseEmitter.send(SseEmitter.event()
                    .name("connected")
                    .data("ok"));
        } catch (IOException e) {
            sseEmitter.completeWithError(e);
        }

        return sseEmitter;
    }

    private SseEmitter handleSseSecurityError(SecurityException e, SseEmitter sseEmitter) {
        try {
            sseEmitter.send(SseEmitter.event()
                    .name("auth-error")
                    .data(Map.of("status", 401)));
        } catch (IOException ex) {
            log.error("Error while sending SSE error event", ex);
        }
        sseEmitter.complete();
        return sseEmitter;
    }

    public void sendNotification(NotificationDTO notificationDTO) {
        applicationEventPublisher.publishEvent(notificationDTO);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    private void publishNotificationEvent(NotificationDTO notificationDTO) {
        for (SseEmitter emitter : sseRegistry.get(notificationDTO.getUserUuid())) {
            try {
                emitter.send(SseEmitter.event()
                        .name(notificationDTO.getEventName())
                        .data(notificationDTO.getData()));
            } catch (IOException e) {
                log.error("Error while sending SSE event to user: {}", notificationDTO.getUserUuid(), e);
                sseRegistry.remove(notificationDTO.getUserUuid(), emitter);
            }
        }
    }
}
