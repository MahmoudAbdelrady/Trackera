package com.mdevs.trackera.dto.worklog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkLogTaskDTO {
    private String taskName;

    private String totalHours;

    private Integer totalMinutes;

    private WorkLogStatus status;

    private boolean hasError = false;

    private Boolean isDeleted;

    public WorkLogTaskDTO(String taskName) {
        this.taskName = taskName;
    }
}
