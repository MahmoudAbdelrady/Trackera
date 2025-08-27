package com.mdevs.trackera.dto.worklog;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class NewWorkLogDTO {
    private String logName;

    @NotNull(message = "Log date is required")
    private LocalDate logDate;

    private Boolean syncToJira;
}
