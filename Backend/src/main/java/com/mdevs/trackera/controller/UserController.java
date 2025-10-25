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
        return new ResponseEntity<>(userService.changePassword(passwordDTO), HttpStatus.OK);
    }

    @GetMapping("/emails")
    public ResponseEntity<?> GetUserEmails() {
        return new ResponseEntity<>(userService.getUserEmails(), HttpStatus.OK);
    }

    @PostMapping("/emails")
    public ResponseEntity<?> AddEmail(@RequestBody Map<String, Object> body) {
        userService.addEmail((String) body.get("email"));
        return new ResponseEntity<>("Email added successfully. Please check your email for verification.", HttpStatus.CREATED);
    }

    @PostMapping("/emails/primary")
    public ResponseEntity<?> MakeEmailPrimary(@RequestBody Map<String, Object> body) {
        userService.makeEmailPrimary((String) body.get("email"));
        return new ResponseEntity<>("Email set as primary successfully.", HttpStatus.OK);
    }

    @PostMapping("/emails/send-verification")
    public ResponseEntity<?> SendVerificationEmail(@RequestBody Map<String, Object> body) {
        userService.sendVerificationEmail((String) body.get("email"));
        return new ResponseEntity<>("Verification email sent successfully. Please check your email.", HttpStatus.OK);
    }

    @DeleteMapping("/emails")
    public ResponseEntity<?> RemoveEmail(@RequestBody Map<String, Object> body) {
        userService.removeEmail((String) body.get("email"));
        return new ResponseEntity<>("Email removed successfully.", HttpStatus.OK);
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
