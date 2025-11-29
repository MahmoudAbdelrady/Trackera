package com.mdevs.trackera.shared.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkLogStatus implements BaseEnum {
    NOT_SYNCED("Not Synced"),
    SYNCED("Synced"),
    PARTIALLY("Partially"),
    SYNC_IN_PROGRESS("Sync In Progress"),
    UNSYNC_IN_PROGRESS("Unsync In Progress");

    private final String label;
}