package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.worklog.WorkLogSyncMessageDTO;
import com.mdevs.trackera.shared.SseRegistry;
import com.mdevs.trackera.shared.annotations.PublicAPI;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.enums.WorkLogSyncMessageType;
import com.mdevs.trackera.utils.CookieHelper;
import com.mdevs.trackera.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
@PublicAPI
public class TestController {
    private final SseRegistry sseRegistry;

    private final RedisTemplate<String, Object> redisTemplate;

    private final JwtUtil jwtUtil;

    @GetMapping("/subscribe")
    public SseEmitter subscribe(@CookieValue(value = CookieHelper.ACCESS_TOKEN_COOKIE_NAME, required = false) String accessToken) {
        return new SseEmitter(0L);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendTestMessage(@RequestParam String logId, @RequestParam WorkLogStatus status) {
        WorkLogSyncMessageDTO syncMessageDTO = new WorkLogSyncMessageDTO(WorkLogSyncMessageType.ALL, logId, null, null, status, null);
        redisTemplate.convertAndSend("worklog-status", syncMessageDTO);
        return ResponseEntity.ok("Message sent");
    }
}
