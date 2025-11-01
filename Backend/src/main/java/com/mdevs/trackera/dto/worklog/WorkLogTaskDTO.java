package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.shared.enums.WorkLogStatus;
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

    private Integer totalMinutes;

    private WorkLogStatus status;

    public WorkLogTaskDTO(String taskName, String taskUrl) {
        this.taskName = taskName;
        this.taskUrl = taskUrl;
    }
}
