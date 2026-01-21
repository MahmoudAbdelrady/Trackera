package com.mdevs.trackera.shared.enums;

import com.mdevs.trackera.dto.jira.JiraProjectDTO;
import com.mdevs.trackera.dto.user.TimezoneOptionDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserPreferenceOption {
    JIRA_PRIMARY_PROJECT("jiraPrimaryProject", JiraProjectDTO.class),
    WORKLOGS_MONTHLY_TARGET_HOURS("worklogsMonthlyTargetHours", Integer.class),
    TIMEZONE("timeZone", TimezoneOptionDTO.class);

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
