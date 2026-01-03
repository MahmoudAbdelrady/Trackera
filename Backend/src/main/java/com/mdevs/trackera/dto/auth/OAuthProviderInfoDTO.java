package com.mdevs.trackera.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthProviderInfoDTO {
    private OAuthProviderDTO provider;

    private boolean isLinked;

    private String email;

    private Boolean isRevoked;

    public OAuthProviderInfoDTO(OAuthProviderDTO provider, boolean isLinked) {
        this.provider = provider;
        this.isLinked = isLinked;
    }
}
