package com.mdevs.trackera.shared.oauth_provider;

import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;

public abstract class OAuthServiceProvider {
    public abstract OAuthUserInfoDTO authenticate(String tokenId);
}
