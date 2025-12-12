package com.mdevs.trackera.dto.worklog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogDetailSyncRequestDTO {
    private String workLogId;

    private Long detailId;

    private String taskName;

    private String jiraId;

    public WorkLogDetailSyncRequestDTO(String taskName, String jiraId) {
        this.taskName = taskName;
        this.jiraId = jiraId;
    }
}
