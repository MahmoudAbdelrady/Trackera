package com.mdevs.trackera.shared.utils;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class TrackeraDateUtil {
    private final static SimpleDateFormat TIME_12_HOUR_FORMAT = new SimpleDateFormat("hh:mm a");

    private final static DateTimeFormatter TIME_12_HOUR_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final static DateTimeFormatter DATE_COMPACT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static SimpleDateFormat getTime12HourFormat() {
        return TIME_12_HOUR_FORMAT;
    }

    public static DateTimeFormatter getTime12HourFormatter() {
        return TIME_12_HOUR_FORMATTER;
    }

    public static DateTimeFormatter getDateCompactFormatter() {
        return DATE_COMPACT_FORMATTER;
    }
}
