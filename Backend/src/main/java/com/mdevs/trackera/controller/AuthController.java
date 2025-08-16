package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.auth.LoginDTO;
import com.mdevs.trackera.dto.auth.OAuthRequestDTO;
import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.dto.auth.SignUpDTO;
import com.mdevs.trackera.service.AuthService;
import com.mdevs.trackera.shared.annotations.PublicAPI;
import com.mdevs.trackera.shared.utils.response.ResponseMaker;
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
        return new ResponseEntity<>(ResponseMaker.makeResponse(authService.signUp(signUpDTO), null), HttpStatus.CREATED);
    }

    @PublicAPI
    @PostMapping("/login")
    public ResponseEntity<?> Login(@RequestBody @Valid LoginDTO loginDTO, HttpServletResponse httpServletResponse) {
        Map<String, Object> result = authService.login(loginDTO, httpServletResponse);
        boolean isError = result.containsKey("isError");
        return new ResponseEntity<>(isError ? ResponseMaker.makeResponse(result.get("message").toString(), null) : result, isError ? HttpStatus.FORBIDDEN : HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/oauth")
    public ResponseEntity<?> OAuth(@RequestBody @Valid OAuthRequestDTO oAuthRequestDTO, HttpServletResponse httpServletResponse) {
        return new ResponseEntity<>(authService.oAuth(oAuthRequestDTO, httpServletResponse), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> Logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        authService.logout(httpServletRequest, httpServletResponse);
        return new ResponseEntity<>(ResponseMaker.makeResponse("Logged out successfully", null), HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/refresh-jwt")
    public ResponseEntity<?> RefreshJwt(@CookieValue(value = "refreshToken") String refreshToken) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(null, authService.refreshJwt(refreshToken)), HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/process-token")
    public ResponseEntity<?> ProcessSecurityToken(@RequestParam String token) {
        try {
            return new ResponseEntity<>(ResponseMaker.makeResponse(null, authService.processToken(token)), HttpStatus.OK);
        } catch (ObjectOptimisticLockingFailureException ex) {
            return new ResponseEntity<>(ResponseMaker.makeResponse(null, Map.of("title", "Token validated successfully")), HttpStatus.OK); // @TODO --> Remove in production
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
        return new ResponseEntity<>(ResponseMaker.makeResponse(authService.sendResetPassword(body.get("email")), null), HttpStatus.OK);
    }

    @PublicAPI
    @PostMapping("/change-password")
    public ResponseEntity<?> ChangePassword(@RequestParam String token, @RequestBody @Valid PasswordDTO passwordDTO) {
        return new ResponseEntity<>(ResponseMaker.makeResponse(authService.changePassword(token, passwordDTO), null), HttpStatus.OK);
    }
}
