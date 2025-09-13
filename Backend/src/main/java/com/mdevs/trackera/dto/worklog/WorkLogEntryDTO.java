package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.entity.WorkLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogEntryDTO {
    private String entryId;

    private LocalTime fromTime;

    private LocalTime toTime;

    private String description;

    private WorkLog.Status status;
}
