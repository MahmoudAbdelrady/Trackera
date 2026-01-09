package com.mdevs.trackera.oauth;

import com.mdevs.trackera.oauth.providers.GoogleOAuthServiceProvider;
import com.mdevs.trackera.oauth.providers.JiraOAuthServiceProvider;
import com.mdevs.trackera.shared.enums.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthProviderFactory {
    private final GoogleOAuthServiceProvider googleOAuthServiceProvider;

    private final JiraOAuthServiceProvider jiraOAuthServiceProvider;

    public OAuthServiceProvider getProvider(OAuthProvider oAuthProvider) {
        switch (oAuthProvider) {
            case GOOGLE -> {
                return googleOAuthServiceProvider;
            }
            case JIRA -> {
                return jiraOAuthServiceProvider;
            }
            default -> {
                return null;
            }
        }
    }
}
