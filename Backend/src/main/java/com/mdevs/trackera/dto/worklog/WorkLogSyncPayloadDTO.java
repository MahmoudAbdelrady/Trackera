package com.mdevs.trackera.dto.worklog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogSyncPayloadDTO {
    private Long userId;

    private Long workLogId;

    private List<WorkLogDetailSyncRequestDTO> detailsToUnsync;

    private List<WorkLogDetailSyncRequestDTO> detailsToSync;

    public WorkLogSyncPayloadDTO(Long userId) {
        this.userId = userId;
    }

    public WorkLogSyncPayloadDTO(Long userId, Long workLogId) {
        this.userId = userId;
        this.workLogId = workLogId;
    }

    public boolean hasWork() {
        return (detailsToSync != null && !detailsToSync.isEmpty()) || (detailsToUnsync != null && !detailsToUnsync.isEmpty());
    }
}
