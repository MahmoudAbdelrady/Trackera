package com.mdevs.trackera.shared.oauth_provider;

import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OAuthProviderFactory {
    private final GoogleOAuthServiceProvider googleOAuthServiceProvider;

    @Autowired
    public OAuthProviderFactory(GoogleOAuthServiceProvider googleOAuthServiceProvider) {
        this.googleOAuthServiceProvider = googleOAuthServiceProvider;
    }

    public OAuthUserInfoDTO getOAuthUserInfoDTO(OAuthRequestDTO oAuthRequestDTO) {
        OAuthProvider providerRequest = EnumUtils.isValidEnum(OAuthProvider.class, oAuthRequestDTO.getProvider()) ? OAuthProvider.valueOf(oAuthRequestDTO.getProvider()) : null;
        if (providerRequest == null) {
            throw new IllegalArgumentException("Invalid OAuth Provider: " + oAuthRequestDTO.getProvider());
        } else {
            return googleOAuthServiceProvider.authenticate(oAuthRequestDTO.getTokenCode());
        }
    }
}
