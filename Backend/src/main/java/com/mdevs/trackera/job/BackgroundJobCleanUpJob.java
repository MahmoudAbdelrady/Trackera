package com.mdevs.trackera.job;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.job.executor.BatchJobExecutor;
import com.mdevs.trackera.service.BackgroundJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BackgroundJobCleanUpJob implements TrackeraJob {
    private final BackgroundJobService backgroundJobService;

    private final BatchJobExecutor batchJobExecutor;

    @Scheduled(cron = "0 0 0 ? * *")
    @Override
    public void execute() {
        batchJobExecutor.execute(
                BackgroundJobCleanUpJob.class.getSimpleName(),
                AppConfig.getBatchJobPageSize(),
                backgroundJobService::deleteFinishedJobs
        );
    }
}
