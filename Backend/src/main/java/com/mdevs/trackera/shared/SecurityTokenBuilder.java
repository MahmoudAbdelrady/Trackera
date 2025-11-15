package com.mdevs.trackera.shared;

import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class SecurityTokenBuilder {
    private User user;

    private String targetEmail;

    private SecurityToken.Type type;

    private String additionalInfo;

    private Map<String, String> extraParameters;

    private String pageUrl;

    private String templateName;
}
