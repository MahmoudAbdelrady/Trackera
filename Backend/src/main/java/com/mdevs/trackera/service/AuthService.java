package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.auth.AuthResultDTO;
import com.mdevs.trackera.dto.auth.LoginDTO;
import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.dto.auth.SignUpDTO;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.repository.SecurityTokenRepository;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.exception.BusinessException;
import com.mdevs.trackera.shared.exception.UnauthorizedException;
import com.mdevs.trackera.shared.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final SecurityTokenService securityTokenService;

    private final SecurityTokenRepository securityTokenRepository;

    private final JwtUtil jwtUtil;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    private final static String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$";

    @Value("${trackera.environment}")
    private String trackeraEnv;

    @Value("${trackera.cookie.max-age}")
    private String cookieMaxAge;

    @Autowired
    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, SecurityTokenService securityTokenService, SecurityTokenRepository securityTokenRepository, JwtUtil jwtUtil, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.securityTokenService = securityTokenService;
        this.securityTokenRepository = securityTokenRepository;
        this.jwtUtil = jwtUtil;
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
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword(), Collections.emptyList());
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

                httpResponse.addCookie(createTrackeraCookie("refreshToken", refreshToken, true));
                result.put("token", accessToken);
            }

            return result;
        } catch (AuthenticationException e) {
            throw new SecurityException("Invalid credentials");
        }
    }

    private Cookie createTrackeraCookie(String name, String value, boolean isHttpOnly) {
        Cookie trackeraCookie = new Cookie(name, value);
        trackeraCookie.setHttpOnly(isHttpOnly);
        trackeraCookie.setSecure(trackeraEnv.equals("prod"));
        trackeraCookie.setPath(isHttpOnly ? "/trackera/auth" : "/");
        trackeraCookie.setMaxAge(Integer.parseInt(cookieMaxAge));
        return trackeraCookie;
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
