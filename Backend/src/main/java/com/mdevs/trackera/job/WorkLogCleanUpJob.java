package com.mdevs.trackera.job;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.job.executor.BatchJobExecutor;
import com.mdevs.trackera.service.WorkLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkLogCleanUpJob implements TrackeraJob {
    private final WorkLogService workLogService;

    private final BatchJobExecutor batchJobExecutor;

    @Scheduled(cron = "0 0 0 ? * *")
    @Override
    public void execute() {
        batchJobExecutor.execute(
                WorkLogCleanUpJob.class.getSimpleName(),
                AppConfig.getBatchJobPageSize(),
                workLogService::deleteDeprecatedWithBatch
        );
    }
}
