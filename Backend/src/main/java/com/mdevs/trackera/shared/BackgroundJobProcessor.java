package com.mdevs.trackera.shared;

import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.job.handlers.BackgroundJobHandler;
import com.mdevs.trackera.shared.exceptions.types.NotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class BackgroundJobProcessor {
    private final Map<String, BackgroundJobHandler> handlers;

    private final Map<String, BackgroundJobHandler> handlerRegistry = new HashMap<>();

    public BackgroundJobProcessor(Map<String, BackgroundJobHandler> handlers) {
        this.handlers = handlers;
    }

    @PostConstruct
    public void init() {
        for (BackgroundJobHandler handler : handlers.values()) {
            handlerRegistry.put((AopUtils.getTargetClass(handler)).getSimpleName(), handler);
        }

        log.info("Registered {} background job handlers", handlerRegistry.size());
    }

    public void process(BackgroundJob job) {
        log.info("Processing job: {} (type: {})", job.getId(), job.getName());
        BackgroundJobHandler jobHandler = handlerRegistry.get(job.getName());
        if (jobHandler == null) {
            throw new NotFoundException("Job not found: " + job.getName());
        }
        jobHandler.handle(job);
    }
}