package com.mdevs.trackera.dto.worklog;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkLogDetailPayloadDTO {
    private String taskName;

    private String entryId;

    @NotNull(message = "Update type is required")
    private Boolean isTask;

    private boolean syncToJira = false;

    private WorkLogDetailNewDataDTO newData;
}
