package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.dto.user.UserPreferenceDTO;
import com.mdevs.trackera.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> GetMeInfo() {
        return ResponseEntity.ok(userService.getMeInfo());
    }

    @GetMapping("/oauth-providers")
    public ResponseEntity<?> GetUserOAuthProviders() {
        return new ResponseEntity<>(userService.getUserOAuthProviders(), HttpStatus.OK);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> ChangePassword(@RequestBody @Valid PasswordDTO passwordDTO) {
        userService.changePassword(passwordDTO);
        return new ResponseEntity<>("Password changed successfully.", HttpStatus.OK);
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> ChangeEmail(@RequestBody Map<String, Object> body) {
        userService.changeEmail((String) body.get("email"));
        return new ResponseEntity<>("We’ve sent a verification link to your new email. Please verify to complete the change.", HttpStatus.CREATED);
    }

    @PostMapping("/send-email-verification")
    public ResponseEntity<?> SendEmailVerification() {
        userService.sendEmailVerification();
        return new ResponseEntity<>("Verification email sent successfully. Please check your email.", HttpStatus.OK);
    }

    @DeleteMapping("/remove-pending-email")
    public ResponseEntity<?> RemovePendingEmail() {
        userService.removePendingEmail();
        return new ResponseEntity<>("Pending email removed successfully.", HttpStatus.OK);
    }

    @GetMapping("/preferences")
    public ResponseEntity<?> GetPreferences() {
        return new ResponseEntity<>(userService.getUserPreferences(), HttpStatus.OK);
    }

    @PostMapping("/preferences")
    public ResponseEntity<?> UpdatePreferences(@RequestBody List<UserPreferenceDTO> userPreferenceDTOList){
        userService.updateUserPreferences(userPreferenceDTOList);
        return new ResponseEntity<>("Preferences updated successfully.", HttpStatus.OK);
    }
}
