package com.mdevs.trackera.dto.worklog;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class WorkLogDetailNewDataDTO {
    private String name;

    private LocalTime startTime;

    private LocalTime endTime;

    private String duration;

    private String description;
}
