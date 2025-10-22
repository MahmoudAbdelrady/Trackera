package com.mdevs.trackera.shared.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EmailTag implements BaseEnum {
    LINKED_TO_GOOGLE("Linked to Google", true),
    LINKED_TO_JIRA("Linked to Jira", true),
    PASSWORD_REQUIRED("Password Required", false);

    private final String label;

    private final boolean oAuthTag;

    EmailTag(String label, Boolean oAuthTag) {
        this.label = label;
        this.oAuthTag = oAuthTag;
    }

    @JsonValue
    public Object toJson() {
        return this.label;
    }
}
