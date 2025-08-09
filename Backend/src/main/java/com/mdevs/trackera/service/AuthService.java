package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.auth.AuthResultDTO;
import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.dto.auth.SignUpDTO;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.exception.BusinessException;
import com.mdevs.trackera.shared.exception.UnauthorizedException;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final UserRepository userRepository;

    private final SecurityTokenService securityTokenService;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    private final static String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$";

    @Autowired
    public AuthService(UserRepository userRepository, SecurityTokenService securityTokenService, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.securityTokenService = securityTokenService;
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
        securityTokenService.deleteSecurityToken(securityToken);

        return new AuthResultDTO(securityToken.getType().getLabel(), message);
    }

    public void validateToken(String token) {
        securityTokenService.getSecurityToken(token);
    }

    @Transactional
    public AuthResultDTO sendResetPassword(String email) {
        validateUserEmail(email);
        User user = userRepository.findByEmail(email);
        if (user != null) {
            Map<String, String> templateParameters = new HashMap<>();
            templateParameters.put("emailTypeDesc", "Please click the link below to reset your password.");
            templateParameters.put("linkLabel", "Reset my password");
            securityTokenService.createAndSendSecurityToken(user, SecurityToken.Type.PASSWORD_RESET, null, templateParameters, "/change-password", "trackera-verification-mail-template");
        }
        return new AuthResultDTO(SecurityToken.Type.PASSWORD_RESET.getLabel(), "If the email exists, a password reset link has been sent to your email.");
    }

    private void validateUserEmail(String email) {
        if (StringUtils.isEmpty(email) || !email.matches(EMAIL_REGEX)) {
            throw new BusinessException("Invalid email format");
        }
    }

    @Transactional
    public AuthResultDTO changePassword(String token, PasswordDTO passwordDTO) {
        SecurityToken securityToken = securityTokenService.getSecurityToken(token);
        if (!securityToken.getType().equals(SecurityToken.Type.PASSWORD_RESET)) {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        User user = securityToken.getUser();
        user.setPassword(getUserNewPassword(user, passwordDTO));
        userRepository.save(user);
        securityTokenService.deleteSecurityToken(securityToken);
        return new AuthResultDTO(SecurityToken.Type.PASSWORD_RESET.getLabel(), "Password changed successfully");
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
