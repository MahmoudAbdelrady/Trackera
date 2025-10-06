package com.mdevs.trackera.shared.enums;

import lombok.Getter;

@Getter
public enum JiraTaskEvaluation implements BaseEnum {
    ON_TIME("On Time"),
    OVERESTIMATED("Overestimated");

    private final String label;

    JiraTaskEvaluation(String label) {
        this.label = label;
    }
}
