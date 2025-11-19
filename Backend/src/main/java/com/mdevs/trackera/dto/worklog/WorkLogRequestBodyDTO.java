package com.mdevs.trackera.dto.worklog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkLogRequestBodyDTO {
    private Object comment;

    private LocalDateTime started;

    private int timeSpentSeconds;
}
