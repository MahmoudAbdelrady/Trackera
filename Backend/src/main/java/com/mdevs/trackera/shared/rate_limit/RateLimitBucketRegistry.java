package com.mdevs.trackera.shared.rate_limit;

import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitBucketRegistry {
    @Getter
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public static final long BUCKET_EXPIRATION_MINUTES = 10;

    public RateLimitBucket add(String compositeKey, int permitsPerMinute, int refillIntervalMinutes) {
        return buckets.computeIfAbsent(compositeKey, _ -> createBucket(permitsPerMinute, refillIntervalMinutes));
    }

    private RateLimitBucket createBucket(int permitsPerMinute, int refillIntervalMinutes) {
        Bucket bucket = Bucket.builder().addLimit(BandwidthBuilder.builder()
                .capacity(permitsPerMinute)
                .refillIntervally(permitsPerMinute, Duration.ofMinutes(refillIntervalMinutes))
                .build()).build();

        return new RateLimitBucket(bucket, System.currentTimeMillis());
    }
}
