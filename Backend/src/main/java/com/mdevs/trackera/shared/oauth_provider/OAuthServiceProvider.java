package com.mdevs.trackera.shared.oauth_provider;

import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;

public abstract class OAuthServiceProvider {
    public abstract String getFlowUrl();

    public abstract OAuthAccessCredentialsDTO getAccessCredentials(String code, boolean isRefresh);

    public abstract OAuthUserInfoDTO authenticate(String code);
}
