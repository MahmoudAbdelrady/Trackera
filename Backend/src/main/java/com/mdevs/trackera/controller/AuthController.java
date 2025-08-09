package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.dto.auth.SignUpDTO;
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
            return new ResponseEntity<>(ResponseMaker.makeResponse(null, Map.of("title", "Token validated successfully")), HttpStatus.OK); // @TODO --> Remove in production
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        authService.validateToken(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-reset-password")
    public ResponseEntity<?> SendResetPassword(@RequestBody Map<String, String> body) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(null, authService.sendResetPassword(body.get("email"))), HttpStatus.OK);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> ChangePassword(@RequestParam String token, @RequestBody @Valid PasswordDTO passwordDTO) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(null, authService.changePassword(token, passwordDTO)), HttpStatus.OK);
    }
}
