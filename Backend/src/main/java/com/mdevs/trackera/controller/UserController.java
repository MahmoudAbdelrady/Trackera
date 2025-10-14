package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> GetMeInfo() {
        return ResponseEntity.ok(userService.getMeInfo());
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> ChangePassword(@RequestBody @Valid PasswordDTO passwordDTO) {
        return new ResponseEntity<>(userService.changePassword(passwordDTO), HttpStatus.OK);
    }
}
