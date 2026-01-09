package com.mdevs.trackera.dto.backgroundjob;

public record BackgroundJobMessageDTO(Long jobId, String jobName, String payload) {
}
