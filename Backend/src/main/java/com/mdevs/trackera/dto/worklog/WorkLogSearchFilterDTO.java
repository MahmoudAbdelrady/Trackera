package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.shared.search_filter.SearchFilter;
import com.mdevs.trackera.shared.enums.WorkLogEvaluation;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
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

    private SearchFilter<BigDecimal> totalHours;

    private WorkLogEvaluation evaluation;

    private WorkLogStatus status;

    public void validate() {
        validateDateFilters();
        validateTotalHoursAndEvaluation();
        if (totalHours != null) {
            totalHours.validate("total hours");
        }
    }

    private void validateDateFilters() {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException("'Date From' must be before 'Date To'");
        }

        LocalDate minDate = AppConfig.getMinQueryableDate();
        if (dateFrom != null && dateFrom.isBefore(minDate)) {
            throw new BusinessException("'Date From' cannot be before " + minDate);
        }

        if (dateTo != null && dateTo.isBefore(minDate)) {
            throw new BusinessException("'Date To' cannot be before " + minDate);
        }
    }

    private void validateTotalHoursAndEvaluation() {
        if (totalHours != null && evaluation != null) {
            throw new BusinessException("Cannot filter by both evaluation and total hours");
        }
    }
}
