package com.mdevs.trackera.dto.auth;

import com.mdevs.trackera.dto.user.UserEmailDTO;
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

    private UserEmailDTO primaryEmail;

    private UserEmailDTO pendingEmail;

    private String profilePicture;

    private String avatarColor;

    private boolean isPasswordSet;
}
