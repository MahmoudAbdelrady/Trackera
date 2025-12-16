package com.mdevs.trackera.shared;

import com.mdevs.trackera.dto.worklog.WorkLogSyncMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

@Slf4j
@Component
public class WorkLogStatusSubscriber {
    private final SseRegistry sseRegistry;

    public WorkLogStatusSubscriber(SseRegistry sseRegistry) {
        this.sseRegistry = sseRegistry;
    }

    public void handleMessage(WorkLogSyncMessageDTO syncMessageDTO) {
        try {
            Set<SseEmitter> userEmitters = sseRegistry.get(syncMessageDTO.getUserUuid());
            syncMessageDTO.setUserUuid(null);
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("log-status")
                            .data(syncMessageDTO));
                } catch (Exception e) {
                    log.error("Error sending SSE event to logId {}: {}", syncMessageDTO.getLogId(), e.getMessage(), e);
                    sseRegistry.remove(syncMessageDTO.getLogId(), emitter);
                }
            }
        } catch (Exception e) {
            log.error("Error processing work log status message: {}", e.getMessage(), e);
        }
    }
}
