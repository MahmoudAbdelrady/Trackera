package com.mdevs.trackera.dto.jira;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessibleResourceDTO {
    private String id;

    private String name;

    private String url;

    private String avatarUrl;

    private String resourceType;

    private String[] scopes;
}
