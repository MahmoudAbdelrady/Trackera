package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.shared.enums.WorkLogStatus;
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

    private WorkLogStatus status;
}
