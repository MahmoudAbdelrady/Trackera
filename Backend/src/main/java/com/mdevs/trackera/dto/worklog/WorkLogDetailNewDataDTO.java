package com.mdevs.trackera.dto.worklog;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class WorkLogDetailNewDataDTO {
    private String name;

    @JsonFormat(pattern = "h:mm a")
    private LocalTime startTime;

    @JsonFormat(pattern = "h:mm a")
    private LocalTime endTime;

    private String duration;

    private String description;
}
