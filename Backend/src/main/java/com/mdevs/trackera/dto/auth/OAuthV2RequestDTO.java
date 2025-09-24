package com.mdevs.trackera.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthV2RequestDTO {
    @NotBlank(message = "Authorization Code is required")
    private String authCode;

    @NotBlank(message = "State is required")
    private String state;
}
