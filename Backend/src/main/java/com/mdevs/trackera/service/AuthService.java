package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.auth.AuthResultDTO;
import com.mdevs.trackera.dto.auth.LoginDTO;
import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.dto.auth.SignUpDTO;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserInvalidToken;
import com.mdevs.trackera.repository.SecurityTokenRepository;
import com.mdevs.trackera.repository.UserInvalidTokenRepository;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.exceptions.BusinessException;
import com.mdevs.trackera.shared.exceptions.UnauthorizedException;
import com.mdevs.trackera.shared.utils.JwtUtil;
import com.mdevs.trackera.shared.utils.TrackeraHasher;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {
    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final SecurityTokenService securityTokenService;

    private final SecurityTokenRepository securityTokenRepository;

    private final UserInvalidTokenRepository userInvalidTokenRepository;

    private final JwtUtil jwtUtil;

    private final TrackeraHasher trackeraHasher;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    private final static String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$";

    private final static String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${trackera.environment}")
    private String trackeraEnv;

    @Value("${trackera.cookie.max-age}")
    private String cookieMaxAge;

    @Autowired
    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, SecurityTokenService securityTokenService, SecurityTokenRepository securityTokenRepository, UserInvalidTokenRepository userInvalidTokenRepository, JwtUtil jwtUtil, TrackeraHasher trackeraHasher, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.securityTokenService = securityTokenService;
        this.securityTokenRepository = securityTokenRepository;
        this.userInvalidTokenRepository = userInvalidTokenRepository;
        this.jwtUtil = jwtUtil;
        this.trackeraHasher = trackeraHasher;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String signUp(SignUpDTO signUpDTO) {
        if (userRepository.existsByEmail(signUpDTO.getEmail())) {
            throw new BusinessException("Email already exists");
        }
        if (!signUpDTO.getPassword().equals(signUpDTO.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }
        User user = modelMapper.map(signUpDTO, User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("emailTypeDesc", "Please click the link below to activate your account.");
        templateParameters.put("linkLabel", "Activate my account");
        securityTokenService.createAndSendSecurityToken(user, SecurityToken.Type.ACCOUNT_ACTIVATION, null, templateParameters, null, "trackera-verification-mail-template");
        return "Account created successfully. Please check your email for verification.";
    }

    @Transactional
    public Map<String, Object> login(LoginDTO loginDTO, HttpServletResponse httpResponse) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword(), List.of());
            Authentication authentication = authenticationManager.authenticate(authToken);
            User loggedUser = (User) authentication.getPrincipal();

            Map<String, Object> result = new HashMap<>();
            if (!loggedUser.isVerified()) {
                String message;
                if (securityTokenRepository.existsByUserAndTypeAndCreatedAtGreaterThanEqual(loggedUser, SecurityToken.Type.ACCOUNT_ACTIVATION, LocalDateTime.now().minusMinutes(SecurityTokenService.MAX_SECURITY_TOKEN_MINUTES))) {
                    message = "Account not activated. Please check your email for the activation link.";
                } else {
                    Map<String, String> templateParameters = new HashMap<>();
                    templateParameters.put("emailTypeDesc", "Please click the link below to activate your account.");
                    templateParameters.put("linkLabel", "Activate my account");
                    securityTokenService.createAndSendSecurityToken(loggedUser, SecurityToken.Type.ACCOUNT_ACTIVATION, null, templateParameters, null, "trackera-verification-mail-template");
                    message = "Account not activated. An activation link has been sent to your email.";
                }
                result.put("isError", true);
                result.put("message", message);
            } else {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String accessToken = jwtUtil.generateToken(loggedUser.getEmail(), true);
                String refreshToken = jwtUtil.generateToken(loggedUser.getEmail(), false);

                httpResponse.addCookie(createTrackeraCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, true, Integer.parseInt(cookieMaxAge)));
                result.put("token", accessToken);
            }

            return result;
        } catch (AuthenticationException e) {
            throw new SecurityException("Invalid credentials");
        }
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String accessToken = httpRequest.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        String refreshToken = httpRequest.getCookies() != null ? Arrays.stream(httpRequest.getCookies()).filter(cookie -> cookie.getName().equals(REFRESH_TOKEN_COOKIE_NAME)).map(Cookie::getValue).findFirst().orElse(null) : null;
        saveInvalidToken(accessToken, true);
        if (!StringUtils.isEmpty(refreshToken)) {
            saveInvalidToken(refreshToken, false);
        }
        httpResponse.addCookie(createTrackeraCookie(REFRESH_TOKEN_COOKIE_NAME, null, true, 0));
    }

    private Cookie createTrackeraCookie(String name, String value, boolean isHttpOnly, int cookieMaxAge) {
        Cookie trackeraCookie = new Cookie(name, value);
        trackeraCookie.setHttpOnly(isHttpOnly);
        trackeraCookie.setSecure(trackeraEnv.equals("prod"));
        trackeraCookie.setPath(isHttpOnly ? "/trackera/auth" : "/");
        trackeraCookie.setMaxAge(cookieMaxAge);
        return trackeraCookie;
    }

    public void saveInvalidToken(String token, boolean isAccessToken) {
        Claims accessTokenClaims = jwtUtil.getTokenPayload(token, isAccessToken);
        User user = userRepository.findByEmail(accessTokenClaims.get("email", String.class));
        Date accessTokenClaimsExpiration = accessTokenClaims.getExpiration();
        UserInvalidToken invalidAccessToken = new UserInvalidToken(user, trackeraHasher.hash(token, false), accessTokenClaimsExpiration, isAccessToken);
        userInvalidTokenRepository.save(invalidAccessToken);
    }

    public Map<String, Object> refreshJwt(String refreshToken) {
        Claims accessTokenClaims = jwtUtil.getTokenPayload(refreshToken, false);
        String userEmail = accessTokenClaims.get("email", String.class);
        if (jwtUtil.isTokenInvalid(userEmail, refreshToken, false)) {
            throw new SecurityException("Invalid or expired refresh token");
        }
        String newAccessToken = jwtUtil.generateToken(userEmail, true);
        return Map.of("token", newAccessToken);
    }

    @Transactional
    public AuthResultDTO processToken(String token) {
        SecurityToken securityToken = securityTokenService.getSecurityToken(token);
        User user = securityToken.getUser();

        String message;
        if (securityToken.getType().equals(SecurityToken.Type.ACCOUNT_ACTIVATION)) {
            user.setVerified(true);
            userRepository.save(user);
            message = "Your account has been successfully activated";
        } else {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        securityTokenRepository.delete(securityToken);

        return new AuthResultDTO(securityToken.getType().getLabel(), message);
    }

    public void validateToken(String token) {
        securityTokenService.getSecurityToken(token);
    }

    @Transactional
    public String sendResetPassword(String email) {
        validateUserEmail(email);
        User user = userRepository.findByEmail(email);
        if (user != null) {
            Map<String, String> templateParameters = new HashMap<>();
            templateParameters.put("emailTypeDesc", "Please click the link below to reset your password.");
            templateParameters.put("linkLabel", "Reset my password");
            securityTokenService.createAndSendSecurityToken(user, SecurityToken.Type.PASSWORD_RESET, null, templateParameters, "/change-password", "trackera-verification-mail-template");
        }
        return "If the email exists, a password reset link has been sent to your email.";
    }

    private void validateUserEmail(String email) {
        if (StringUtils.isEmpty(email) || !email.matches(EMAIL_REGEX)) {
            throw new BusinessException("Invalid email format");
        }
    }

    @Transactional
    public String changePassword(String token, PasswordDTO passwordDTO) {
        SecurityToken securityToken = securityTokenService.getSecurityToken(token);
        if (!securityToken.getType().equals(SecurityToken.Type.PASSWORD_RESET)) {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        User user = securityToken.getUser();
        user.setPassword(getUserNewPassword(user, passwordDTO));
        userRepository.save(user);
        securityTokenRepository.delete(securityToken);
        return "Password changed successfully";
    }

    public String getUserNewPassword(User user, PasswordDTO passwordDTO) {
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmNewPassword())) {
            throw new BusinessException("Passwords do not match");
        }
        if (passwordEncoder.matches(passwordDTO.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password cannot be the same as the current password");
        }
        return passwordEncoder.encode(passwordDTO.getNewPassword());
    }
}
