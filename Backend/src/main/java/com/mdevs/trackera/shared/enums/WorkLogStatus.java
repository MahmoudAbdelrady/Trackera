package com.mdevs.trackera.shared.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkLogStatus implements BaseEnum {
    SYNCED("Synced"),
    PARTIALLY("Partially"),
    NOT_SYNCED("Not Synced");

    private final String label;
}