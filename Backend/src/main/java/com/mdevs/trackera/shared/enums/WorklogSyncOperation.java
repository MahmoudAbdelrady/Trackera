package com.mdevs.trackera.shared.enums;

import lombok.Getter;

public enum WorklogSyncOperation implements BaseEnum {
    SYNC("Sync"),
    UNSYNC("Unsync"),
    RESYNC("Resync");

    @Getter
    private final String label;

     WorklogSyncOperation(String label) {
        this.label = label;
    }
}
