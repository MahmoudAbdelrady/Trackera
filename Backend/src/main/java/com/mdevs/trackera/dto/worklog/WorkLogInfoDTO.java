package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.entity.WorkLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class WorkLogInfoDTO {
    private Long id;

    private String name;

    private String totalHours;

    private LocalDate workDate;

    private WorkLog.Evaluation evaluation;

    private WorkLog.Status status;
}
