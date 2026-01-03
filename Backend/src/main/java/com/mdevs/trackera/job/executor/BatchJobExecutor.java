package com.mdevs.trackera.job.executor;

import com.mdevs.trackera.config.general.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BatchJobExecutor {
    public void execute(String jobName, int pageSize, BatchJobOperation jobOperation) {
        log.info("Starting {} job", jobName);

        long maxId = 0;
        int successfulBatches = 0;
        int consecutiveFailures = 0;
        int failedBatches = 0;
        int maxJobFailures = AppConfig.getMaxJobFailures();

        do {
            try {
                maxId = jobOperation.executeBatch(maxId, pageSize);
                if (maxId != -1) {
                    successfulBatches++;
                    consecutiveFailures = 0; // Reset on success
                }
            } catch (Exception e) {
                log.error("Error while executing {} job in batch: {} Error:\n{}", jobName, maxId, e.getMessage(), e);
                maxId += pageSize; // Skip to next batch on error
                failedBatches++;
                consecutiveFailures++;
                if (consecutiveFailures >= maxJobFailures) {
                    log.error("Maximum job failures reached ({}). Aborting {} job.", maxJobFailures, jobName);
                    break;
                }
            }
        } while (maxId != -1);

        log.info("{} job completed. Successful batches: {}, Failed: {}", jobName, successfulBatches, failedBatches);
    }
}
