package com.mdevs.trackera.dto.worklog;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkLogDetailResponseDTO {
    private String message;
    private WorkLogInfoDTO worklogInfo;
    private WorkLogTaskDTO task;
    private WorkLogEntryDTO entry;
}
