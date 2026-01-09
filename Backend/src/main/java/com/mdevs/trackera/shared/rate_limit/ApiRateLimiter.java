package com.mdevs.trackera.shared.rate_limit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiRateLimiter {
    private final RateLimitBucketRegistry rateLimitBucketRegistry;

    public boolean tryConsume(String key, String endpoint, int permitsPerMinute, int refillIntervalMinutes) {
        String compositeKey = key + ":" + endpoint;
        log.info("[Rate Limiter] Consuming request: {}", compositeKey);
        RateLimitBucket rateLimitBucket = rateLimitBucketRegistry.add(compositeKey, permitsPerMinute, refillIntervalMinutes);
        rateLimitBucket.updateLastAccess();
        return rateLimitBucket.getBucket().tryConsume(1);
    }
}
