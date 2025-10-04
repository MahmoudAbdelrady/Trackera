package com.mdevs.trackera.shared.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Map;

@Getter
public enum JiraTaskEvaluation implements BaseEnum {
    ON_TIME("On Time"),
    OVERESTIMATED("Overestimated");

    private final String label;

    JiraTaskEvaluation(String label) {
        this.label = label;
    }

    @JsonValue
    public Map<String, String> toJson() {
        return Map.of("label", label, "value", this.name());
    }
}
