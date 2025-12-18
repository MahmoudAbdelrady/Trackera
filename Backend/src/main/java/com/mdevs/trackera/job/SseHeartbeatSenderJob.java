package com.mdevs.trackera.job;

import com.mdevs.trackera.shared.SseRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class SseHeartbeatSenderJob {
    private final SseRegistry sseRegistry;

    public SseHeartbeatSenderJob(SseRegistry sseRegistry) {
        this.sseRegistry = sseRegistry;
    }

    @Scheduled(fixedRate = 15_000)
    public void sendHeartbeats() {
        if (sseRegistry.getEmitters().isEmpty()) {
            return;
        }
        log.info("Number of available SSE connections: {}", sseRegistry.getEmitters().size());
        sseRegistry.getEmitters().forEach((userUuid, emitterSet) -> CompletableFuture.runAsync(() -> {
            try {
                log.info("Sending heartbeat ping to userUuid: {}", userUuid);
                for (SseEmitter emitter : emitterSet) {
                    emitter.send(SseEmitter.event().comment("ping"));
                }
            } catch (Exception e) {
                log.warn("Failed to send heartbeat ping to userUuid: {}. Removing emitter. Error: {}", userUuid, e.getMessage());
                emitterSet.forEach(em -> sseRegistry.remove(userUuid, em));
            }
        }));
    }
}
