package com.mdevs.trackera.job;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.service.SecurityTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityTokenCleanUpJob {
    private final SecurityTokenService securityTokenService;

    public SecurityTokenCleanUpJob(SecurityTokenService securityTokenService) {
        this.securityTokenService = securityTokenService;
    }

    @Scheduled(cron = "0 0 0 ? * *")
    public void cleanUpExpiredTokens() {
        log.info("Starting security token cleanup job");

        long maxId = 0;
        int pageSize = 100;
        int successfulBatches = 0;
        int consecutiveFailures = 0;
        int failedBatches = 0;
        int maxJobFailures = AppConfig.getMaxJobFailures();

        do {
            try {
                maxId = securityTokenService.deleteExpiredTokensBatch(maxId, pageSize);
                if (maxId != -1) {
                    successfulBatches++;
                    consecutiveFailures = 0; // Reset on success
                }
            } catch (Exception e) {
                log.error("Error while deleting expired security request tokens in batch: {} Error:\n{}", maxId, e.getMessage(), e);
                maxId += pageSize; // Skip to next batch on error
                failedBatches++;
                consecutiveFailures++;
                if (consecutiveFailures >= maxJobFailures) {
                    log.error("Maximum job failures reached ({}). Aborting cleanup job.", maxJobFailures);
                    break;
                }
            }
        } while (maxId != -1);

        log.info("Security token cleanup completed. Successful batches: {}, Failed: {}", successfulBatches, failedBatches);
    }
}
