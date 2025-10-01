package com.mdevs.trackera.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public OAuthUserInfoDTO(String email, String firstname, String lastname, String profilePicture) {
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.profilePicture = profilePicture;
    }
}
