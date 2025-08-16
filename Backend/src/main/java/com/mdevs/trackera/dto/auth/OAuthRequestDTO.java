package com.mdevs.trackera.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthRequestDTO {
    @NotBlank(message = "Token Code is required")
    private String tokenCode;

    @NotBlank(message = "OAuth Provider is required")
    private String provider;
}
