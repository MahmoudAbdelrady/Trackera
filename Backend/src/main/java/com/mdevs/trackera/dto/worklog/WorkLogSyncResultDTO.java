package com.mdevs.trackera.dto.worklog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogSyncResultDTO {
    private boolean hasSoftError;

    private String hardError;

    public boolean hasError() {
        return !StringUtils.isEmpty(hardError) || hasSoftError;
    }
}
