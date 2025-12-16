package com.mdevs.trackera.shared;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseRegistry {
    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void add(String userUuid, SseEmitter emitter) {
        emitters.computeIfAbsent(userUuid, _ -> ConcurrentHashMap.newKeySet())
                .add(emitter);
    }

    public void remove(String userUuid, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(userUuid);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emitters.remove(userUuid);
            }
        }
    }

    public Set<SseEmitter> get(String userUuid) {
        return emitters.getOrDefault(userUuid, Set.of());
    }
}
