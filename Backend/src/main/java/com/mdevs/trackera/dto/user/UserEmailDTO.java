package com.mdevs.trackera.dto.user;

import com.mdevs.trackera.entity.UserEmail;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserEmailDTO {
    private String email;

    private boolean verified;

    public UserEmailDTO(UserEmail userEmail) {
        this.email = userEmail.getEmail();
        this.verified = userEmail.isVerified();
    }
}
