package com.mdevs.trackera.shared;

import com.mdevs.trackera.entity.BackgroundJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BackgroundJobProcessor {
    public void process(BackgroundJob job) {
        log.info("Processing job: {} (type: {})", job.getId(), job.getName());
        System.out.println("Count: " + job.getRetryCount());
        if (job.getRetryCount() < 2) {
            throw new RuntimeException("Simulated job failure for testing retries.");
        }
        printHelloWorld(job);
    }

    private void printHelloWorld(BackgroundJob job) {
        log.info("Hello, World! from job: {} (type: {})", job.getId(), job.getName());
    }
}