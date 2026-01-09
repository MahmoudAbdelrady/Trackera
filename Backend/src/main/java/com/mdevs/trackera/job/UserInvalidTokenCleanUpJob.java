package com.mdevs.trackera.job;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.job.executor.BatchJobExecutor;
import com.mdevs.trackera.service.UserInvalidTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserInvalidTokenCleanUpJob implements TrackeraJob {
    private final UserInvalidTokenService userInvalidTokenService;

    private final BatchJobExecutor batchJobExecutor;

    @Scheduled(cron = "0 0 0 ? * *")
    @Override
    public void execute() {
        batchJobExecutor.execute(
                UserInvalidTokenCleanUpJob.class.getSimpleName(),
                AppConfig.getBatchJobPageSize(),
                userInvalidTokenService::deleteExpiredTokensBatch
        );
    }
}
