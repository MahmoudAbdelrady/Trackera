package com.mdevs.trackera.dto.auth;

import com.mdevs.trackera.shared.enums.OAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthProviderDTO {
    private String displayName;

    private String code;

    public static OAuthProviderDTO from(OAuthProvider provider) {
        return new OAuthProviderDTO(provider.getDisplayName(), provider.getCode());
    }
}
