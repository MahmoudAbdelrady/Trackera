package com.mdevs.trackera.shared.oauth_provider;

import com.mdevs.trackera.shared.enums.EmailTag;
import lombok.Getter;

@Getter
public enum OAuthProvider {
    GOOGLE("google", "Google", EmailTag.LINKED_TO_GOOGLE),
    JIRA("jira", "Jira", EmailTag.LINKED_TO_JIRA);

    private final String code;

    private final String displayName;

    private final EmailTag emailTag;

    OAuthProvider(String code, String displayName, EmailTag emailTag) {
        this.code = code;
        this.displayName = displayName;
        this.emailTag = emailTag;
    }

    public static OAuthProvider fromCode(String value) {
        for (OAuthProvider provider : OAuthProvider.values()) {
            if (provider.getCode().equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unsupported OAuth Provider: " + value);
    }
}
