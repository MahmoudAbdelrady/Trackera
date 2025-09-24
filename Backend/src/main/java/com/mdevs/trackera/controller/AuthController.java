package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.auth.*;
import com.mdevs.trackera.service.AuthService;
import com.mdevs.trackera.shared.annotations.PublicAPI;
import com.mdevs.trackera.shared.exceptions.ExceptionResponseMaker;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @PublicAPI
    @PostMapping("/signup")
    public ResponseEntity<?> SignUp(@RequestBody @Valid SignUpDTO signUpDTO) {
        return new ResponseEntity<>(authService.signUp(signUpDTO), HttpStatus.CREATED);
    }

    @PublicAPI
    @PostMapping("/login")
    public ResponseEntity<?> Login(@RequestBody @Valid LoginDTO loginDTO, HttpServletResponse httpServletResponse) {
        Map<String, Object> result = authService.login(loginDTO, httpServletResponse);
        return result.containsKey("isError") ? ExceptionResponseMaker.makeResponse(result.get("message").toString(), HttpStatus.FORBIDDEN) : new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/oauth")
    public ResponseEntity<?> OAuth(@RequestBody @Valid OAuthRequestDTO oAuthRequestDTO, HttpServletResponse httpServletResponse) {
        return new ResponseEntity<>(authService.oAuth(oAuthRequestDTO, httpServletResponse), HttpStatus.OK);
    }

    @PublicAPI
    @GetMapping("/oauth-v2/{oAuthProvider}")
    public ResponseEntity<?> OAuthV2(@PathVariable String oAuthProvider, HttpServletRequest httpServletRequest) {
        return new ResponseEntity<>(authService.oAuthV2(oAuthProvider, httpServletRequest), HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/oauth-v2/{oAuthProvider}/callback")
    public void OAuthV2Callback(@PathVariable String oAuthProvider, @RequestBody @Valid OAuthV2RequestDTO oAuthV2RequestDTO) {
        authService.oAuthV2Callback(oAuthProvider, oAuthV2RequestDTO);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> Logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        authService.logout(httpServletRequest, httpServletResponse);
        return new ResponseEntity<>("Logged out successfully", HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/refresh-jwt")
    public ResponseEntity<?> RefreshJwt(@CookieValue(value = "refreshToken") String refreshToken) {
        return new ResponseEntity<>(authService.refreshJwt(refreshToken), HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/process-token")
    public ResponseEntity<?> ProcessSecurityToken(@RequestParam String token) {
        try {
            return new ResponseEntity<>(authService.processToken(token), HttpStatus.OK);
        } catch (ObjectOptimisticLockingFailureException ex) {
            return new ResponseEntity<>(new AuthResultDTO("Token validated successfully", null), HttpStatus.OK); // @TODO --> Remove in production
        }
    }

    @PublicAPI
    @PostMapping("/validate-token")
    public ResponseEntity<?> ValidateSecurityToken(@RequestParam String token) {
        authService.validateToken(token);
        return ResponseEntity.ok().build();
    }

    @PublicAPI
    @PostMapping("/send-reset-password")
    public ResponseEntity<?> SendResetPassword(@RequestBody Map<String, String> body) {
        return new ResponseEntity<>(authService.sendResetPassword(body.get("email")), HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/change-password")
    public ResponseEntity<?> ChangePassword(@RequestParam String token, @RequestBody @Valid PasswordDTO passwordDTO) {
        return new ResponseEntity<>(authService.changePassword(token, passwordDTO), HttpStatus.OK);
    }
}
