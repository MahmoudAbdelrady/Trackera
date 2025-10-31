package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.dto.OperatorSearchDTO;
import com.mdevs.trackera.shared.enums.WorkLogEvaluation;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class WorkLogSearchFilterDTO {
    private String logName;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private OperatorSearchDTO<BigDecimal> totalHours;

    private WorkLogEvaluation evaluation;

    private OperatorSearchDTO<WorkLogStatus> status;
}
