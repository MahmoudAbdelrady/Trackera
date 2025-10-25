package com.mdevs.trackera.shared.oauth_provider;

import com.mdevs.trackera.shared.oauth_provider.providers.GoogleOAuthServiceProvider;
import com.mdevs.trackera.shared.oauth_provider.providers.JiraOAuthServiceProvider;
import org.springframework.stereotype.Component;

@Component
public class OAuthProviderFactory {
    private final GoogleOAuthServiceProvider googleOAuthServiceProvider;

    private final JiraOAuthServiceProvider jiraOAuthServiceProvider;

    public OAuthProviderFactory(GoogleOAuthServiceProvider googleOAuthServiceProvider, JiraOAuthServiceProvider jiraOAuthServiceProvider) {
        this.googleOAuthServiceProvider = googleOAuthServiceProvider;
        this.jiraOAuthServiceProvider = jiraOAuthServiceProvider;
    }

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
