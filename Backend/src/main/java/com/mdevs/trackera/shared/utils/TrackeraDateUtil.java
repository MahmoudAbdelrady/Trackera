package com.mdevs.trackera.shared.utils;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class TrackeraDateUtil {
    private final static SimpleDateFormat SIMPLE_12H_FORMAT = new SimpleDateFormat("hh:mm a");

    private final static DateTimeFormatter DATE_TIME_12H_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final static DateTimeFormatter COMPACTED_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static SimpleDateFormat getSimple12hFormat() {
        return SIMPLE_12H_FORMAT;
    }

    public static DateTimeFormatter getDateTime12hFormatter() {
        return DATE_TIME_12H_FORMATTER;
    }

    public static DateTimeFormatter getCompactedDateFormatter() {
        return COMPACTED_DATE_FORMATTER;
    }
}
