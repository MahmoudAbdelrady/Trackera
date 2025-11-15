package com.mdevs.trackera.dto.jira;

import com.mdevs.trackera.shared.enums.JiraTaskEvaluation;

public record JiraTaskDTO(
        String taskName,
        String taskUrl,
        StatusDTO status,
        boolean isResolved,
        ProjectDTO project,
        TimeTrackingDTO timeTracking) {
    public record StatusDTO(String name, String category) {
    }

    public record ProjectDTO(String name, String icon) {
    }

    public record TimeTrackingDTO(
            String originalEstimate,
            String loggedTime,
            String remainingTime,
            JiraTaskEvaluation evaluation,
            String notes
    ) {
    }
}
