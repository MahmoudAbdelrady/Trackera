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

    private DateTimeUtil() {}

    public static DateTimeFormatter getDateTime12hFormatter() {
        return DATE_TIME_12H_FORMATTER;
    }

    public static DateTimeFormatter getCompactedDateFormatter() {
        return COMPACTED_DATE_FORMATTER;
    }

    public static LocalDateTime convertDateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), SYSTEM_ZONE);
    }

    public static List<String> getAvailableTimezoneIds() {
        Instant now = Instant.now();
        return ZoneId.getAvailableZoneIds().stream()
                .filter(DateTimeUtil::isCommonTimezone)
                .map(ZoneId::of)
                .sorted(Comparator.comparing(z -> z.getRules().getOffset(now)))
                .map(ZoneId::getId).toList();
    }

    public static List<TimezoneOptionDTO> getAvailableTimezones() {
        Instant now = Instant.now();
        return ZoneId.getAvailableZoneIds().stream()
                .filter(DateTimeUtil::isCommonTimezone)
                .map(ZoneId::of)
                .sorted(Comparator.comparing(z -> z.getRules().getOffset(now)))
                .map(zoneId -> {
                    ZoneOffset offset = zoneId.getRules().getOffset(now);
                    String label = String.format("(GMT%s) %s", offset.getId(), zoneId.getId());
                    return new TimezoneOptionDTO(zoneId.getId(), label);
                })
                .toList();
    }

    private static boolean isCommonTimezone(String zoneId) {
        if (!zoneId.contains("/")) return false;

        return (zoneId.startsWith("Africa/") ||
                zoneId.startsWith("America/") ||
                zoneId.startsWith("Asia/") ||
                zoneId.startsWith("Australia/") ||
                zoneId.startsWith("Europe/") ||
                zoneId.startsWith("Atlantic/")) && (!zoneId.equalsIgnoreCase("Asia/Tel_Aviv"));
    }
}
