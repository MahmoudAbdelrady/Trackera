package com.mdevs.trackera.utils;

import com.mdevs.trackera.dto.user.TimezoneOptionDTO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public final class DateTimeUtil {
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private static final DateTimeFormatter DATE_TIME_12H_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter COMPACTED_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter SIMPLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {}

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

    public static List<String> getAvailableTimezoneIds() {
        Instant now = Instant.now();
        return ZoneId.getAvailableZoneIds().stream()
                .map(ZoneId::of)
                .sorted(Comparator.comparing(z -> z.getRules().getOffset(now)))
                .map(ZoneId::getId).toList();
    }

    public static List<TimezoneOptionDTO> getAvailableTimezones() {
        Instant now = Instant.now();
        return ZoneId.getAvailableZoneIds().stream()
                .map(ZoneId::of)
                .sorted(Comparator.comparing(z -> z.getRules().getOffset(now)))
                .map(zoneId -> {
                    ZoneOffset offset = zoneId.getRules().getOffset(now);
                    String label = String.format("(GMT%s) %s", offset.getId(), zoneId.getId());
                    return new TimezoneOptionDTO(zoneId.getId(), label);
                })
                .toList();
    }
}
