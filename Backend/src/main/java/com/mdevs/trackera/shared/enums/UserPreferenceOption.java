package com.mdevs.trackera.shared.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserPreferenceOption {
    JIRA_PRIMARY_PROJECT("jiraPrimaryProject", Object.class),
    WORKLOGS_MONTHLY_TARGET_HOURS("worklogsMonthlyTargetHours", Integer.class);

    private final String code;

    private final Class<?> valueType;

    public static UserPreferenceOption fromCode(String code) {
        for (UserPreferenceOption option : UserPreferenceOption.values()) {
            if (option.getCode().equals(code)) {
                return option;
            }
        }
        throw new IllegalArgumentException("Invalid UserPreferenceOption code: " + code);
    }
}
