package com.mdevs.trackera.shared.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class TrackeraDateUtil {
    private final static SimpleDateFormat SIMPLE_12H_FORMAT = new SimpleDateFormat("hh:mm a");

    private final static DateTimeFormatter DATE_TIME_12H_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final static DateTimeFormatter COMPACTED_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static class DurationParts {
        int days;
        int hours;
        int minutes;

        DurationParts(int days, int hours, int minutes) {
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
        }
    }

    public static SimpleDateFormat getSimple12hFormat() {
        return SIMPLE_12H_FORMAT;
    }

    public static DateTimeFormatter getDateTime12hFormatter() {
        return DATE_TIME_12H_FORMATTER;
    }

    public static DateTimeFormatter getCompactedDateFormatter() {
        return COMPACTED_DATE_FORMATTER;
    }

    // (hours + minutes only)
    public static String formatDuration(BigDecimal totalHours) {
        DurationParts parts = extractDurationParts(totalHours, false);

        if (parts.hours == 0 && parts.minutes == 0) {
            return "0h";
        }
        return parts.minutes == 0 ? String.format("%dh", parts.hours) : String.format("%dh %dm", parts.hours, parts.minutes);
    }

    // (days + hours + minutes)
    public static String formatDurationWithDays(BigDecimal totalHours) {
        DurationParts parts = extractDurationParts(totalHours, true);

        if (parts.days == 0 && parts.hours == 0 && parts.minutes == 0) {
            return "0h";
        }

        StringBuilder result = new StringBuilder();
        if (parts.days > 0) result.append(parts.days).append("d ");
        if (parts.hours > 0) result.append(parts.hours).append("h ");
        if (parts.minutes > 0) result.append(parts.minutes).append("m");

        return result.toString().trim();
    }

    private static DurationParts extractDurationParts(BigDecimal totalHours, boolean includeDays) {
        if (totalHours == null) {
            return new DurationParts(0, 0, 0);
        }

        int hours = totalHours.intValue();
        BigDecimal fractionalPart = totalHours.subtract(BigDecimal.valueOf(hours));

        int minutes = fractionalPart.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).intValue();

        if (minutes == 60) {
            hours += 1;
            minutes = 0;
        }

        int days = 0;
        if (includeDays) {
            days = hours / 8;
            hours = hours % 8;
            if (hours == 0 && minutes == 0 && totalHours.compareTo(BigDecimal.ZERO) > 0) {
                // exactly multiple of 8h -> count as a full day
                days += 1;
            }
        }

        return new DurationParts(days, hours, minutes);
    }
}
