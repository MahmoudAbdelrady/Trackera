package com.mdevs.trackera.job;

import com.mdevs.trackera.shared.rate_limit.RateLimitBucketRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitBucketCleanUpJob implements TrackeraJob {
    private final RateLimitBucketRegistry rateLimitBucketRegistry;

    @Scheduled(cron = "0 */5 * * * *")
    @Override
    public void execute() {
        if (rateLimitBucketRegistry.getBuckets().isEmpty()) {
            return;
        }
        log.info("Starting Rate Limit Bucket cleanup job. Current bucket count: {}", rateLimitBucketRegistry.getBuckets().size());

        long timeOut = System.currentTimeMillis() - Duration.ofMinutes(RateLimitBucketRegistry.BUCKET_EXPIRATION_MINUTES).toMillis();
        int removedCnt = rateLimitBucketRegistry.getBuckets().size();
        rateLimitBucketRegistry.getBuckets().entrySet().removeIf(entry -> entry.getValue().getLastAccessed() <= timeOut);
        removedCnt -= rateLimitBucketRegistry.getBuckets().size();

        log.info("Rate Limit Bucket cleanup job completed. Removed: {}", removedCnt);
    }
}
