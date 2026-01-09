package com.mdevs.trackera.dto.auth;

import com.mdevs.trackera.shared.enums.OAuthProvider;

public record OAuthProviderDTO(String displayName, String code) {

    public static OAuthProviderDTO from(OAuthProvider provider) {
        return new OAuthProviderDTO(provider.getDisplayName(), provider.getCode());
    }
}
