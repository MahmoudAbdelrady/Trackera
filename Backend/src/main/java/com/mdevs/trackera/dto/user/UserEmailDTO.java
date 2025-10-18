package com.mdevs.trackera.dto.user;

import com.mdevs.trackera.shared.enums.EmailTag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserEmailDTO {
    private String email;

    private boolean isPrimary;

    private boolean verified;

    private boolean isOAuthLinked;

    private List<EmailTag> tags;
}
