package com.mdevs.trackera.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserInfoDTO {
    private Long userId;

    private String email;

    private String firstname;

    private String lastname;

    private String profilePicture;

    private OAuthAccessCredentialsDTO accessCredentials;

    private Map<String, Object> additionalInfo;
}
