package com.mdevs.trackera.shared;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

@Component
@Slf4j
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return type.cast(value);
        } catch (Exception e) {
            log.error("Error while fetching cached data with cache key: {}", key, e);
            return null;
        }
    }

    public boolean exists(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Error while checking existence of cache key: {}", key, e);
            return false;
        }
    }

    public void evict(String... keys) {
        redisTemplate.delete(Arrays.asList(keys));
    }

    public void evictPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void evictAll() {
        Set<String> keys = redisTemplate.keys("*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
