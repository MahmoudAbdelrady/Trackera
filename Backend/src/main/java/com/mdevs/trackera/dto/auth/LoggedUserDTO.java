package com.mdevs.trackera.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoggedUserDTO {
    private String firstname;

    private String lastname;

    private String profilePicture;

    private String avatarColor;

    private boolean isPasswordSet;
}
