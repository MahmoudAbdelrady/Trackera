package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.entity.WorkLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogTaskDTO {
    private String taskName;

    private String taskUrl;

    private String totalHours;

    private WorkLog.Status status;
}
