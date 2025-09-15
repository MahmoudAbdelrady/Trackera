package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.entity.WorkLog;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkLogTaskDTO {
    private String taskName;

    private String taskUrl;

    private String totalHours;

    private WorkLog.Status status;

    public WorkLogTaskDTO(String taskName, String taskUrl) {
        this.taskName = taskName;
        this.taskUrl = taskUrl;
    }
}
