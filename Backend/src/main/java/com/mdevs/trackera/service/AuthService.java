package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.user.SignUpDTO;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.exception.BusinessException;
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
        templateParameters.put("emailType", SecurityToken.Type.ACCOUNT_ACTIVATION.getLabel());
        templateParameters.put("emailTypeDesc", "Please click the link below to activate your account.");
        templateParameters.put("linkLabel", "Activate my account");
        securityTokenService.createAndSendSecurityToken(user, SecurityToken.Type.ACCOUNT_ACTIVATION, null, templateParameters, null, "Account Activation", "trackera-verification-mail-template");
        return "Account created successfully. Please check your email for verification.";
    }

    @Transactional
    public String processToken(String token) {
        SecurityToken securityToken = securityTokenService.getSecurityToken(token);
        User user = securityToken.getUser();

        String message = "";
        if (securityToken.getType().equals(SecurityToken.Type.ACCOUNT_ACTIVATION)) {
            user.setVerified(true);
            userRepository.save(user);
            message = "Account activated successfully.";
        }
        securityTokenService.deleteSecurityToken(securityToken);
        return message;
    }
}
