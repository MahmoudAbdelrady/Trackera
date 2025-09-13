package com.mdevs.trackera.shared.enums;

import lombok.Getter;

import java.time.LocalTime;

@Getter
public enum WorkLogColumn implements BaseEnum {
    ROW_NUMBER("Row Number", String.class),
    TASK_NAME("Task Name", String.class),
    FROM_HOUR("From Hour", LocalTime.class),
    TO_HOUR("To Hour", LocalTime.class),
    DURATION("Duration", String.class),
    DESCRIPTION("Description", String.class);

    private final String label;
    private final Class<?> resultType;

    WorkLogColumn(String label, Class<?> resultType) {
        this.label = label;
        this.resultType = resultType;
    }
}
