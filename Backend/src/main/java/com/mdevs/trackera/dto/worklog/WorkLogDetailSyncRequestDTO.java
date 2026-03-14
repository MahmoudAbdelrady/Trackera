package com.mdevs.trackera.dto.worklog;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkLogDetailSyncRequestDTO {
    private Long detailId;

    private String taskName;

    private String jiraId;

    private String oldTaskName;

    public WorkLogDetailSyncRequestDTO(Long detailId, String taskName, String jiraId) {
        this.detailId = detailId;
        this.taskName = taskName;
        this.jiraId = jiraId;
    }

    public WorkLogDetailSyncRequestDTO(String taskName, String jiraId) {
        this.taskName = taskName;
        this.jiraId = jiraId;
    }
}
