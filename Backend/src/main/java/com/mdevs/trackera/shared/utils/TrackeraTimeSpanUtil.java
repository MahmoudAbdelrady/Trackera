package com.mdevs.trackera.shared.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class TrackeraTimeSpanUtil {
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
        if (totalHours == null || totalHours.compareTo(BigDecimal.ZERO) <= 0) {
            return new DurationParts(0, 0, 0);
        }

        BigDecimal totalMinutesBD = totalHours.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP);

        long totalMinutes = totalMinutesBD.longValueExact();

        int days = 0;
        int hours;
        int minutes;

        if (includeDays) {
            long minutesPerDay = 8L * 60L;
            days = (int) (totalMinutes / minutesPerDay);
            long rem = totalMinutes % minutesPerDay;
            hours = (int) (rem / 60);
            minutes = (int) (rem % 60);
        } else {
            hours = (int) (totalMinutes / 60);
            minutes = (int) (totalMinutes % 60);
        }

        return new DurationParts(days, hours, minutes);
    }
}
