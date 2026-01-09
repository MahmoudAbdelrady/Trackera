package com.mdevs.trackera.dto.worklog;

import java.util.List;

public record WorklogSummaryResultDTO(List<WorkLogSummaryDTO> currentMonthSummary, String previousMonthLoggedHours) {
}
