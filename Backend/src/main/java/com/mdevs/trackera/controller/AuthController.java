package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.auth.*;
import com.mdevs.trackera.dto.user.PasswordDTO;
import com.mdevs.trackera.service.AuthService;
import com.mdevs.trackera.service.SecurityTokenService;
import com.mdevs.trackera.shared.annotations.PublicAPI;
import com.mdevs.trackera.shared.annotations.RateLimited;
import com.mdevs.trackera.utils.CookieHelper;
import com.mdevs.trackera.utils.ExceptionResponseMaker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    private final SecurityTokenService securityTokenService;

    @PublicAPI
    @RateLimited
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody @Valid SignUpDTO signUpDTO) {
        authService.signUp(signUpDTO);
        return new ResponseEntity<>("Account created successfully. Please check your email for verification.", HttpStatus.CREATED);
    }

    @PublicAPI
    @RateLimited
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO loginDTO, HttpServletResponse response) {
        Map<String, Object> result = authService.login(loginDTO, response);
        return result != null && result.containsKey("isError") ? ExceptionResponseMaker.makeResponse(result.get("message").toString(), HttpStatus.FORBIDDEN) : new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PublicAPI
    @RateLimited
    @GetMapping("/oauth/{providerCode}")
    public ResponseEntity<Map<String, String>> oAuth(@PathVariable String providerCode, @RequestParam(required = false, defaultValue = "false") boolean forceLink, HttpServletRequest request) {
        return new ResponseEntity<>(Map.of("url", authService.oAuth(providerCode, forceLink, request)), HttpStatus.OK);
    }

    @PublicAPI
    @RateLimited
    @PostMapping("/oauth/{providerCode}/callback")
    public ResponseEntity<Map<String, Object>> oAuthCallback(@PathVariable String providerCode, @RequestBody @Valid OAuthRequestDTO oAuthRequestDTO, HttpServletResponse response) {
        return new ResponseEntity<>(authService.oAuthCallback(providerCode, oAuthRequestDTO, response), HttpStatus.OK);
    }

    @RateLimited
    @PostMapping("/oauth/unlink/{providerCode}")
    public ResponseEntity<String> unlinkOAuthProvider(@PathVariable String providerCode) {
        return new ResponseEntity<>(authService.unlinkOAuthProvider(providerCode), HttpStatus.OK);
    }

    @RateLimited
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return new ResponseEntity<>("Logged out successfully", HttpStatus.OK);
    }

    @PublicAPI
    @GetMapping("/session")
    public ResponseEntity<Void> getSession(@CookieValue(value = CookieHelper.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        authService.getSession(refreshToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PublicAPI
    @RateLimited
    @PostMapping("/jwt/refresh")
    public ResponseEntity<Void> refreshJwt(@CookieValue(value = CookieHelper.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
                                        @CookieValue(value = CookieHelper.CSRF_COOKIE_NAME, required = false) String csrfCookieToken,
                                        HttpServletRequest request, HttpServletResponse response) {
        authService.refreshJwt(refreshToken, csrfCookieToken, request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PublicAPI
    @RateLimited
    @PostMapping("/token/consume")
    public ResponseEntity<AuthResultDTO> consumeSecurityToken(@RequestParam String token) {
        return new ResponseEntity<>(authService.consumeToken(token), HttpStatus.OK);
    }

    @PublicAPI
    @RateLimited
    @PostMapping("/token/validate")
    public ResponseEntity<Void> validateSecurityToken(@RequestParam String token) {
        securityTokenService.validateAndGet(token);
        return ResponseEntity.ok().build();
    }

    @PublicAPI
    @RateLimited
    @PostMapping("/password/request-reset")
    public ResponseEntity<String> requestResetPassword(@RequestBody Map<String, String> body) {
        authService.requestResetPassword(body.get("email"));
        return new ResponseEntity<>("If the email exists, a password reset link has been sent to your email.", HttpStatus.OK);
    }

    @PublicAPI
    @RateLimited
    @PostMapping("/password")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestBody @Valid PasswordDTO passwordDTO) {
        authService.resetUserPassword(token, passwordDTO);
        return new ResponseEntity<>("Password changed successfully", HttpStatus.OK);
    }
}
