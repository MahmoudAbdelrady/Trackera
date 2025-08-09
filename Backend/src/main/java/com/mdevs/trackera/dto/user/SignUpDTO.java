package com.mdevs.trackera.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpDTO {
    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[A-Za-z]{2,50}$", message = "First name must be between 2 and 50 characters long and contain only letters")
    private String firstname;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^(?=.{2,120}$)[A-Za-z]+(?: [A-Za-z]+)*$", message = "Last name must be between 2 and 120 characters long and contain only letters and spaces")
    private String lastname;

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$", message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,}$", message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
