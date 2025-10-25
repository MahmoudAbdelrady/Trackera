package com.mdevs.trackera.dto.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessibleResourceDTO {
    private String id;

    private String name;

    private String url;

    private String avatarUrl;
}
