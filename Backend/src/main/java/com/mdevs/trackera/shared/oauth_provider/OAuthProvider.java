package com.mdevs.trackera.shared.oauth_provider;

import com.mdevs.trackera.shared.enums.BaseEnum;

public enum OAuthProvider implements BaseEnum {
    GOOGLE("google"),
    JIRA("jira");

    private final String label;

    OAuthProvider(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public static OAuthProvider fromLabel(String value) {
        for (OAuthProvider provider : OAuthProvider.values()) {
            if (provider.getLabel().equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unsupported OAuth Provider: " + value);
    }
}
