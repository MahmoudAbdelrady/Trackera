package com.mdevs.trackera.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;

@Getter
@RequiredArgsConstructor
public enum WorkLogColumn implements BaseEnum {
    ROW_NUMBER("Row Number", -1, String.class),
    TASK_NAME("Task Name", 0, String.class),
    FROM_HOUR("From Hour", 1, LocalTime.class),
    TO_HOUR("To Hour", 2, LocalTime.class),
    DESCRIPTION("Description", 3, String.class);

    private final String label;

    private final Integer index;

    private final Class<?> resultType;
}
