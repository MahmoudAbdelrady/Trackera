package com.mdevs.trackera.job;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.job.executor.BatchJobExecutor;
import com.mdevs.trackera.service.SecurityTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityTokenCleanUpJob implements TrackeraJob {
    private final SecurityTokenService securityTokenService;

    private final BatchJobExecutor batchJobExecutor;

    @Scheduled(cron = "0 0 0 ? * *")
    @Override
    public void execute() {
        batchJobExecutor.execute(
                SecurityTokenCleanUpJob.class.getSimpleName(),
                AppConfig.getBatchJobPageSize(),
                securityTokenService::deleteExpiredTokensBatch
        );
    }
}
