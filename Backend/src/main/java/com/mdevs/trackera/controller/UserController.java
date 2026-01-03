package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.user.PasswordDTO;
import com.mdevs.trackera.service.UserPreferenceService;
import com.mdevs.trackera.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    private final UserPreferenceService userPreferenceService;

    @GetMapping("/me")
    public ResponseEntity<?> GetMeInfo() {
        return ResponseEntity.ok(userService.getMeInfo());
    }

    @GetMapping("/oauth-providers")
    public ResponseEntity<?> GetUserOAuthProviders() {
        return new ResponseEntity<>(userService.getUserOAuthProviders(), HttpStatus.OK);
    }

    @PostMapping("/password")
    public ResponseEntity<?> ChangePassword(@RequestBody @Valid PasswordDTO passwordDTO) {
        userService.changePassword(passwordDTO);
        return new ResponseEntity<>("Password changed successfully.", HttpStatus.OK);
    }

    @PostMapping("/email/request-change")
    public ResponseEntity<?> RequestEmailChange(@RequestBody Map<String, Object> body) {
        userService.requestEmailChange((String) body.get("email"));
        return new ResponseEntity<>("We’ve sent a verification link to your new email. Please verify to complete the change.", HttpStatus.CREATED);
    }

    @PostMapping("/email/send-verification")
    public ResponseEntity<?> SendEmailVerification() {
        userService.sendEmailVerification();
        return new ResponseEntity<>("Verification email sent successfully. Please check your email.", HttpStatus.OK);
    }

    @DeleteMapping("/email/pending")
    public ResponseEntity<?> RemovePendingEmail() {
        userService.removePendingEmail();
        return new ResponseEntity<>("Pending email removed successfully.", HttpStatus.OK);
    }

    @GetMapping("/preferences")
    public ResponseEntity<?> GetPreferences() {
        return new ResponseEntity<>(userPreferenceService.getAll(), HttpStatus.OK);
    }

    @PostMapping("/preferences")
    public ResponseEntity<?> UpdatePreferences(@RequestBody Map<String, Object> updatedPreferences){
        userService.updateUserPreferences(updatedPreferences);
        return new ResponseEntity<>("Preferences updated successfully.", HttpStatus.OK);
    }
}
