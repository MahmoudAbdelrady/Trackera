package com.mdevs.trackera.job;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.job.executor.BatchJobExecutor;
import com.mdevs.trackera.service.SecurityTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityTokenCleanUpJob {
    private final SecurityTokenService securityTokenService;

    private final BatchJobExecutor batchJobExecutor;

    public SecurityTokenCleanUpJob(SecurityTokenService securityTokenService, BatchJobExecutor batchJobExecutor) {
        this.securityTokenService = securityTokenService;
        this.batchJobExecutor = batchJobExecutor;
    }

    @Scheduled(cron = "0 0 0 ? * *")
    public void cleanUpExpiredTokens() {
        batchJobExecutor.execute(
                SecurityTokenCleanUpJob.class.getSimpleName(),
                AppConfig.getBatchJobPageSize(),
                securityTokenService::deleteExpiredTokensBatch
        );
    }
}
