package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.user.PasswordDTO;
import com.mdevs.trackera.service.UserPreferenceService;
import com.mdevs.trackera.service.UserService;
import com.mdevs.trackera.shared.annotations.RateLimited;
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
    public ResponseEntity<?> getMeInfo() {
        return ResponseEntity.ok(userService.getMeInfo());
    }

    @GetMapping("/oauth-providers")
    public ResponseEntity<?> getUserOAuthProviders() {
        return new ResponseEntity<>(userService.getUserOAuthProviders(), HttpStatus.OK);
    }

    @RateLimited
    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody @Valid PasswordDTO passwordDTO) {
        userService.changePassword(passwordDTO);
        return new ResponseEntity<>("Password changed successfully.", HttpStatus.OK);
    }

    @RateLimited
    @PostMapping("/email/request-change")
    public ResponseEntity<?> requestEmailChange(@RequestBody Map<String, Object> body) {
        userService.requestEmailChange((String) body.get("email"));
        return new ResponseEntity<>("We’ve sent a verification link to your new email. Please verify to complete the change.", HttpStatus.CREATED);
    }

    @RateLimited
    @PostMapping("/email/send-verification")
    public ResponseEntity<?> sendEmailVerification() {
        userService.sendEmailVerification();
        return new ResponseEntity<>("Verification email sent successfully. Please check your email.", HttpStatus.OK);
    }

    @RateLimited
    @DeleteMapping("/email/pending")
    public ResponseEntity<?> removePendingEmail() {
        userService.removePendingEmail();
        return new ResponseEntity<>("Pending email removed successfully.", HttpStatus.OK);
    }

    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences() {
        return new ResponseEntity<>(userPreferenceService.getAll(), HttpStatus.OK);
    }

    @RateLimited(permitsPerMinute = 20)
    @PostMapping("/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody Map<String, Object> updatedPreferences) {
        userService.updateUserPreferences(updatedPreferences);
        return new ResponseEntity<>("Preferences updated successfully.", HttpStatus.OK);
    }
}
