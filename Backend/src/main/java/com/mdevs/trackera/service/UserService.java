package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.LoggedUserDTO;
import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    private final SecurityTokenService securityTokenService;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, SecurityTokenService securityTokenService, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.securityTokenService = securityTokenService;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("Account not found. Please create an account and try again.");
        }
        return user;
    }

    public LoggedUserDTO getMeInfo() {
        return modelMapper.map(AppConfig.getCurrentUser(), LoggedUserDTO.class);
    }

    @Transactional
    public String changePassword(PasswordDTO passwordDTO) {
        User user = Objects.requireNonNull(AppConfig.getCurrentUser());
        if (!StringUtils.isEmpty(passwordDTO.getCurrentPassword()) && (!user.isPasswordSet() || !passwordEncoder.matches(passwordDTO.getCurrentPassword(), user.getPassword()))) {
            throw new BusinessException("Current password is incorrect.");
        }
        user.setPassword(getUserNewPassword(user, passwordDTO));
        sendResetPasswordEmail(user, "Your password has been changed. If you did not perform this action, please reset your password immediately.", false);
        userRepository.save(user);
        return "Password changed successfully.";
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

    public void sendResetPasswordEmail(User user, String description, boolean isForReset) {
        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("emailTypeDesc", description);
        templateParameters.put("linkLabel", "Reset my password");
        securityTokenService.createAndSendSecurityToken(user, isForReset ? SecurityToken.Type.PASSWORD_RESET : SecurityToken.Type.PASSWORD_CHANGE, null, templateParameters, "/change-password", "trackera-verification-mail-template");
    }
}
