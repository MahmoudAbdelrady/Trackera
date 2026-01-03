package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.auth.*;
import com.mdevs.trackera.service.AuthService;
import com.mdevs.trackera.service.SecurityTokenService;
import com.mdevs.trackera.shared.annotations.PublicAPI;
import com.mdevs.trackera.utils.CookieHelper;
import com.mdevs.trackera.utils.ExceptionResponseMaker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    private final SecurityTokenService securityTokenService;

    @PublicAPI
    @PostMapping("/signup")
    public ResponseEntity<?> SignUp(@RequestBody @Valid SignUpDTO signUpDTO) {
        authService.signUp(signUpDTO);
        return new ResponseEntity<>("Account created successfully. Please check your email for verification.", HttpStatus.CREATED);
    }

    @PublicAPI
    @PostMapping("/login")
    public ResponseEntity<?> Login(@RequestBody @Valid LoginDTO loginDTO, HttpServletResponse response) {
        Map<String, Object> result = authService.login(loginDTO, response);
        return result.containsKey("isError") ? ExceptionResponseMaker.makeResponse(result.get("message").toString(), HttpStatus.FORBIDDEN) : new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PublicAPI
    @GetMapping("/oauth/{providerCode}")
    public ResponseEntity<?> OAuth(@PathVariable String providerCode, @RequestParam(required = false, defaultValue = "false") boolean forceLink, HttpServletRequest request) {
        return new ResponseEntity<>(Map.of("url", authService.oAuth(providerCode, forceLink, request)), HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/oauth/{providerCode}/callback")
    public ResponseEntity<?> OAuthCallback(@PathVariable String providerCode, @RequestBody @Valid OAuthRequestDTO oAuthRequestDTO, HttpServletResponse response) {
        return new ResponseEntity<>(authService.oAuthCallback(providerCode, oAuthRequestDTO, response), HttpStatus.OK);
    }

    @PostMapping("/oauth/unlink/{providerCode}")
    public ResponseEntity<?> UnlinkOAuthProvider(@PathVariable String providerCode) {
        return new ResponseEntity<>(authService.unlinkOAuthProvider(providerCode), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> Logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return new ResponseEntity<>("Logged out successfully", HttpStatus.OK);
    }

    @PublicAPI
    @GetMapping("/session")
    public ResponseEntity<?> GetSession(@CookieValue(value = CookieHelper.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        authService.getSession(refreshToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/jwt/refresh")
    public ResponseEntity<?> RefreshJwt(@CookieValue(value = CookieHelper.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken, HttpServletResponse response) {
        authService.refreshJwt(refreshToken, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/token/consume")
    public ResponseEntity<?> ConsumeSecurityToken(@RequestParam String token) {
        try {
            return new ResponseEntity<>(authService.consumeToken(token), HttpStatus.OK);
        } catch (ObjectOptimisticLockingFailureException ex) {
            return new ResponseEntity<>(new AuthResultDTO("Token validated successfully", null), HttpStatus.OK); // @TODO --> Remove in production
        }
    }

    @PublicAPI
    @PostMapping("/token/validate")
    public ResponseEntity<?> ValidateSecurityToken(@RequestParam String token) {
        securityTokenService.validateAndGet(token);
        return ResponseEntity.ok().build();
    }

    @PublicAPI
    @PostMapping("/password/request-reset")
    public ResponseEntity<?> RequestResetPassword(@RequestBody Map<String, String> body) {
        authService.requestResetPassword(body.get("email"));
        return new ResponseEntity<>("If the email exists, a password reset link has been sent to your email.", HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/password")
    public ResponseEntity<?> ResetPassword(@RequestParam String token, @RequestBody @Valid PasswordDTO passwordDTO) {
        authService.resetUserPassword(token, passwordDTO);
        return new ResponseEntity<>("Password changed successfully", HttpStatus.OK);
    }
}
