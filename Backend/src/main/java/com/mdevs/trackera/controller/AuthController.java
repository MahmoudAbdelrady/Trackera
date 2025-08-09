package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.user.SignUpDTO;
import com.mdevs.trackera.service.AuthService;
import com.mdevs.trackera.shared.utils.response.ResponseMaker;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> SignUp(@RequestBody @Valid SignUpDTO signUpDTO) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(authService.signUp(signUpDTO), null), HttpStatus.CREATED);
    }

    @PostMapping("/process-token")
    public ResponseEntity<?> ProcessToken(@RequestParam String token) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(null, authService.processToken(token)), HttpStatus.OK);
    }
}
