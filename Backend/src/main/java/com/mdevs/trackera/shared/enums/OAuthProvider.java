package com.mdevs.trackera.shared.enums;

import lombok.Getter;

@Getter
public enum OAuthProvider {
    GOOGLE("google", "Google"),
    JIRA("jira", "Jira");

    private final String code;

    private final String displayName;

    OAuthProvider(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
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
