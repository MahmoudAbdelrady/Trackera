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

    private String workLogUuid;

    private List<WorkLogDetailSyncRequestDTO> detailsToSync;

    private List<WorkLogDetailSyncRequestDTO> detailsToUnsync;

    private List<WorkLogDetailSyncRequestDTO> detailsToReSync;

    public WorkLogSyncPayloadDTO(Long userId) {
        this.userId = userId;
    }

    public WorkLogSyncPayloadDTO(Long userId, String workLogUuid) {
        this.userId = userId;
        this.workLogUuid = workLogUuid;
    }

    public List<WorkLogDetailSyncRequestDTO> getDetailsToSync() {
        return detailsToSync != null ? detailsToSync : List.of();
    }

    public List<WorkLogDetailSyncRequestDTO> getDetailsToUnsync() {
        return detailsToUnsync != null ? detailsToUnsync : List.of();
    }

    public List<WorkLogDetailSyncRequestDTO> getDetailsToReSync() {
        return detailsToReSync != null ? detailsToReSync : List.of();
    }

    public boolean hasWork() {
        return !getDetailsToSync().isEmpty() || !getDetailsToUnsync().isEmpty() || !getDetailsToReSync().isEmpty();
    }

    public boolean isWorkLogDeletion() {
        return getDetailsToUnsync().stream().anyMatch(d -> d.getDetailId() == null);
    }
}
