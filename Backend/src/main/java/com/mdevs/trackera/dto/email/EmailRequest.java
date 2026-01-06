package com.mdevs.trackera.dto.email;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String targetEmail;

    private String subject;

    private String templateName;

    private Map<String, String> parameters;

    private boolean isHtml = true;
}