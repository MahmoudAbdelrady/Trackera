package com.mdevs.trackera.dto.worklog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import com.mdevs.trackera.shared.enums.WorkLogSyncMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkLogSyncMessageDTO {
    private String userUuid;

    private WorkLogSyncMessageType type;

    private String logId;

    private List<String> taskNames;

    private List<String> entryIds;

    private WorkLogStatus status;

    private String syncError;

    public WorkLogSyncMessageDTO(WorkLogSyncMessageType type, String logId, List<String> taskNames, List<String> entryIds, WorkLogStatus status) {
        this.type = type;
        this.logId = logId;
        this.taskNames = taskNames;
        this.entryIds = entryIds;
        this.status = status;
    }
}