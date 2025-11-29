package com.mdevs.trackera.dto.backgroundjob;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BackgroundJobMessageDTO {
    private Long jobId;

    private String jobName;

    private String payload;
}
