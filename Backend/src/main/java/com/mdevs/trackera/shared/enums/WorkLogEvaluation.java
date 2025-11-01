package com.mdevs.trackera.shared.enums;

import com.mdevs.trackera.utils.TrackeraTimeSpanUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum WorkLogEvaluation implements BaseEnum {
    EXCELLENT("Excellent", BigDecimal.valueOf(8), null),
    GOOD("Good", BigDecimal.valueOf(7.5), BigDecimal.valueOf(8)),
    MODERATE("Moderate", BigDecimal.valueOf(7), BigDecimal.valueOf(7.5)),
    POOR("Poor", null, BigDecimal.valueOf(7));

    private final String label;

    private final BigDecimal minHours;

    private final BigDecimal maxHours;

    public Integer getMinMinutes() {
        return TrackeraTimeSpanUtil.hoursToMinutes(minHours);
    }

    public Integer getMaxMinutes() {
        return TrackeraTimeSpanUtil.hoursToMinutes(maxHours);
    }

    public boolean matches(Integer totalMinutes) {
        if (totalMinutes == null) {
            return false;
        }

        boolean meetsMin = minHours == null || totalMinutes >= getMinMinutes();
        boolean meetsMax = maxHours == null || totalMinutes < getMaxMinutes();

        return meetsMin && meetsMax;
    }

    public static WorkLogEvaluation fromTotalMinutes(Integer totalMinutes) {
        return Arrays.stream(values()).filter(e -> e.matches(totalMinutes)).findFirst().orElse(POOR);
    }
}
