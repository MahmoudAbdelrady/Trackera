package com.mdevs.trackera.shared;

import com.mdevs.trackera.shared.search_filter.SearchFilter;
import com.mdevs.trackera.shared.enums.WorkLogEvaluation;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.search_filter.SearchOperator;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class WorkLogQueryBuilder {
    private final StringBuilder query;

    @Getter
    private final Map<String, Object> parameters;

    public WorkLogQueryBuilder(Long userId) {
        this.query = new StringBuilder("SELECT wl FROM WorkLog wl WHERE wl.user.id = :userId");
        this.parameters = new HashMap<>();
        this.parameters.put("userId", userId);
    }

    public WorkLogQueryBuilder withLogName(String logName) {
        if (StringUtils.isEmpty(logName)) return this;

        query.append(" AND LOWER(wl.name) LIKE LOWER(CONCAT('%', :logName, '%'))");
        parameters.put("logName", logName);
        return this;
    }

    public WorkLogQueryBuilder withDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null) {
            query.append(" AND wl.workDate >= :dateFrom");
            parameters.put("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            query.append(" AND wl.workDate <= :dateTo");
            parameters.put("dateTo", dateTo);
        }
        return this;
    }

    public WorkLogQueryBuilder withTotalHours(SearchFilter<BigDecimal> totalHoursFilter) {
        if (totalHoursFilter == null) return this;

        query.append(" AND wl.totalMinutes ").append(totalHoursFilter.getOperator().getQuerySymbol()).append(" :totalHours");
        parameters.put("totalHours", DurationFormatter.hoursToMinutes(totalHoursFilter.getValue()));
        if (totalHoursFilter.getOperator().equals(SearchOperator.BETWEEN)) {
            query.append(" AND :totalHoursSecond");
            parameters.put("totalHoursSecond", DurationFormatter.hoursToMinutes(totalHoursFilter.getSecondValue()));
        }
        return this;
    }

    public WorkLogQueryBuilder withEvaluation(WorkLogEvaluation evaluation) {
        if (evaluation == null) return this;

        if (evaluation.getMinHours() != null && evaluation.getMaxHours() != null) {
            query.append(" AND wl.totalMinutes >= :minMinutes AND wl.totalMinutes < :maxMinutes");
            parameters.put("minMinutes", evaluation.getMinMinutes());
            parameters.put("maxMinutes", evaluation.getMaxMinutes());
        } else if (evaluation.getMinHours() != null) {
            query.append(" AND wl.totalMinutes >= :minMinutes");
            parameters.put("minMinutes", evaluation.getMinMinutes());
        } else if (evaluation.getMaxHours() != null) {
            query.append(" AND wl.totalMinutes < :maxMinutes");
            parameters.put("maxMinutes", evaluation.getMaxMinutes());
        }
        return this;
    }

    public WorkLogQueryBuilder withStatus(WorkLogStatus statusFilter) {
        if (statusFilter == null) return this;

        query.append(" AND wl.status = :status");
        parameters.put("status", statusFilter);
        return this;
    }

    public String getQuery() {
        query.append(" ORDER BY wl.workDate DESC");
        return query.toString();
    }
}
