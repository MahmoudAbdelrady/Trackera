package com.mdevs.trackera.job;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.job.executor.BatchJobExecutor;
import com.mdevs.trackera.service.UserInvalidTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserInvalidTokenCleanUpJob {
    private final UserInvalidTokenService userInvalidTokenService;

    private final BatchJobExecutor batchJobExecutor;

    public UserInvalidTokenCleanUpJob(UserInvalidTokenService userInvalidTokenService, BatchJobExecutor batchJobExecutor) {
        this.userInvalidTokenService = userInvalidTokenService;
        this.batchJobExecutor = batchJobExecutor;
    }

    @Scheduled(cron = "0 0 0 ? * *")
    public void cleanUpExpiredTokens() {
        batchJobExecutor.execute(
                UserInvalidTokenCleanUpJob.class.getSimpleName(),
                AppConfig.getBatchJobPageSize(),
                userInvalidTokenService::deleteExpiredTokensBatch
        );
    }
}
