package com.mdevs.trackera.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class DateTimeUtils {
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private static final DateTimeFormatter DATE_TIME_12H_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter COMPACTED_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter SIMPLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtils() {}

    public static DateTimeFormatter getDateTime12hFormatter() {
        return DATE_TIME_12H_FORMATTER;
    }

    public static DateTimeFormatter getCompactedDateFormatter() {
        return COMPACTED_DATE_FORMATTER;
    }

    public static DateTimeFormatter getSimpleDateTimeFormatter() {
        return SIMPLE_DATE_TIME_FORMATTER;
    }

    public static LocalDateTime convertDateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), SYSTEM_ZONE);
    }
}
