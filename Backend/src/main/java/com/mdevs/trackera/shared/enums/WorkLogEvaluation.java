package com.mdevs.trackera.shared.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public enum WorkLogEvaluation implements BaseEnum {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    MODERATE("Moderate"),
    POOR("Poor");

    private final String label;

    public static WorkLogEvaluation fromTotalHours(BigDecimal totalHours) {
        if (totalHours.compareTo(BigDecimal.valueOf(8)) >= 0) {
            return EXCELLENT;
        } else if (totalHours.compareTo(BigDecimal.valueOf(7.5)) >= 0) {
            return GOOD;
        } else if (totalHours.compareTo(BigDecimal.valueOf(7)) >= 0) {
            return MODERATE;
        } else {
            return POOR;
        }
    }
}
