package com.mdevs.trackera.dto.email;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
public class EmailRequest {
    private String targetEmail;

    private String subject;

    private String templateName;

    private Map<String, String> parameters;

    @Builder.Default
    private boolean isHtml = true;
}