package com.mdevs.trackera.job.handlers;

import com.mdevs.trackera.entity.BackgroundJob;

public interface BackgroundJobHandler {
    void handle(BackgroundJob job);
}
