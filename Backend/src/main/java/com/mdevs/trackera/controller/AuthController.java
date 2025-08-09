package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.user.SignUpDTO;
import com.mdevs.trackera.service.AuthService;
import com.mdevs.trackera.shared.utils.response.ResponseMaker;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        try {
            return new ResponseEntity<>(ResponseMaker.makeResponse(null, authService.processToken(token)), HttpStatus.OK);
        } catch (ObjectOptimisticLockingFailureException ex) {
            return new ResponseEntity<>(ResponseMaker.makeResponse(null, Map.of("title", "Token validated successfully")), HttpStatus.OK); // Handle optimistic locking failure with idempotent response
        }
    }
}
