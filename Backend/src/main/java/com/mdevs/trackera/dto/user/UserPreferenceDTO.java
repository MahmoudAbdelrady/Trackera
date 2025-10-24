package com.mdevs.trackera.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPreferenceDTO {
    @NotBlank(message = "Preference key is required")
    private String key;

    @NotBlank(message = "Preference value is required")
    private String value;
}
