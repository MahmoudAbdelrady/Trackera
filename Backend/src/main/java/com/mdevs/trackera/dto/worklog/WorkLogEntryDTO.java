package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.entity.WorkLog;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkLogEntryDTO {
    private String id;

    private String fromTime;

    private String toTime;

    private String duration;

    private String description;

    private WorkLog.Status status;
}
