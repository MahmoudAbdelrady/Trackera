package com.mdevs.trackera.job.executor;

@FunctionalInterface
public interface BatchJobOperation {
    long executeBatch(long lastProcessedId, int batchSize);
}
