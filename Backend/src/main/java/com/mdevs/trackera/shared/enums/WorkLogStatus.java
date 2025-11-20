package com.mdevs.trackera.shared.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkLogStatus implements BaseEnum {
    NOT_SYNCED("Not Synced"),
    SYNCED("Synced"),
    SYNCING("Syncing"),
    PARTIALLY("Partially");

    private final String label;
}