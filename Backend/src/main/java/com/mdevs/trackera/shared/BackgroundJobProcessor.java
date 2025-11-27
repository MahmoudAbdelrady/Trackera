package com.mdevs.trackera.shared;

import com.mdevs.trackera.entity.BackgroundJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BackgroundJobProcessor {
    public void process(BackgroundJob job) {
        log.info("Processing job: {} (type: {})", job.getId(), job.getName());
        printHelloWorld(job);
    }

    private void printHelloWorld(BackgroundJob job) {
        log.info("Hello, World! from job: {} (type: {})", job.getId(), job.getName());
    }
}