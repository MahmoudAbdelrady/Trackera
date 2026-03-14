package com.mdevs.trackera.dto.worklog;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateWorkLogDetailResponseDTO {
    private String message;
    private WorkLogInfoDTO worklogInfo;
    private WorkLogTaskDTO currentTask;
    private WorkLogTaskDTO newTask;
    private WorkLogEntryDTO entry;

    public UpdateWorkLogDetailResponseDTO(String message) {
        this.message = message;
    }
}
