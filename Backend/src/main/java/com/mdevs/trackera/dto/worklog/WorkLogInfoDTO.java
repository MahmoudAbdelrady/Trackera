package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.shared.enums.WorkLogEvaluation;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class WorkLogInfoDTO {
    private String id;

    private String name;

    private String totalHours;

    private LocalDate workDate;

    private WorkLogEvaluation evaluation;

    private WorkLogStatus status;
}
