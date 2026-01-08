package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.OAuthProviderDTO;
import com.mdevs.trackera.dto.auth.OAuthProviderInfoDTO;
import com.mdevs.trackera.dto.email.EmailRequest;
import com.mdevs.trackera.dto.user.LoggedUserDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.user.PasswordDTO;
import com.mdevs.trackera.dto.auth.SignUpDTO;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import com.mdevs.trackera.entity.OAuthConnection;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.EmailService;
import com.mdevs.trackera.shared.EmailTemplates;
import com.mdevs.trackera.shared.SecurityTokenBuilder;
import com.mdevs.trackera.shared.enums.UserPreferenceOption;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.enums.OAuthProvider;
import com.mdevs.trackera.shared.exceptions.types.NotFoundException;
import com.mdevs.trackera.shared.mappers.UserMapper;
import com.mdevs.trackera.utils.JsonUtil;
import com.mdevs.trackera.utils.EmailTemplateUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    private final UserEmailService userEmailService;

    private final OAuthConnectionService oAuthConnectionService;

    private final UserPreferenceService userPreferenceService;

    private final JiraService jiraService;

    private final SecurityTokenService securityTokenService;

    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;

    //<editor-fold desc="User Find Methods">
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return Optional.ofNullable(userRepository.findByPrimaryEmail(email)).orElseThrow(() -> new UsernameNotFoundException("Account not found."));
    }

    public User findByIdOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User findByUuidOrThrow(String uuid) {
        return Optional.ofNullable(userRepository.findByUuid(uuid)).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User findByPrimaryEmail(String email) {
        return userRepository.findByPrimaryEmail(email);
    }
    //</editor-fold>

    //<editor-fold desc="User Info Retrieval">
    public LoggedUserDTO getMeInfo() {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        LoggedUserDTO loggedUserDTO = userMapper.toLoggedUserDTO(loggedUser);
        loggedUserDTO.setJiraLinked(oAuthConnectionService.isConnected(loggedUser, OAuthProvider.JIRA));
        return loggedUserDTO;
    }

    public List<OAuthProviderInfoDTO> getUserOAuthProviders() {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        Map<OAuthProvider, OAuthConnection> linkedProviders = oAuthConnectionService.getConnectionsAsMap(loggedUser);

        return Arrays.stream(OAuthProvider.values()).map(provider -> {
            OAuthConnection userProvider = linkedProviders.get(provider);
            boolean isLinked = userProvider != null;
            OAuthProviderInfoDTO providerInfoDTO = new OAuthProviderInfoDTO(OAuthProviderDTO.from(provider), isLinked);
            if (isLinked) {
                providerInfoDTO.setEmail(userProvider.getAccountEmail().getEmail());
                providerInfoDTO.setIsRevoked(userProvider.isRevoked());
            }
            return providerInfoDTO;
        }).toList();
    }
    //</editor-fold>

    //<editor-fold desc="User Info Management">
    public void updateUserPreferences(Map<String, Object> updatedPreferences) {
        User currentUser = AppConfig.getAuthenticatedCurrentUser();
        for (Map.Entry<String, Object> preference : updatedPreferences.entrySet()) {
            userPreferenceService.validatePreference(preference);
            if (preference.getKey().equals(UserPreferenceOption.JIRA_PRIMARY_PROJECT.getCode())) {
                oAuthConnectionService.validateAndGetConnection(currentUser, OAuthProvider.JIRA);
                preference.setValue(JsonUtil.convertObjectToJsonString(jiraService.findSiteById(currentUser, preference.getValue().toString())));
            }
        }
        userPreferenceService.updateAll(currentUser, updatedPreferences);
        jiraService.handleSiteChange(currentUser);
    }
    //</editor-fold>

    //<editor-fold desc="Registration & Authentication">
    @Transactional
    public User create(SignUpDTO signUpDTO) {
        userEmailService.ensureEmailAvailable(signUpDTO.getEmail());

        if (!signUpDTO.getPassword().equals(signUpDTO.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        User user = userMapper.toEntity(signUpDTO);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        UserEmail userEmail = userEmailService.create(user, signUpDTO.getEmail());
        user.setPrimaryEmail(userEmail);
        userRepository.save(user);

        userPreferenceService.create(user, UserPreferenceOption.WORKLOGS_MONTHLY_TARGET_HOURS, "200");

        return user;
    }

    @Transactional
    public Map<String, Object> createOrGetOAuthUser(OAuthUserInfoDTO oAuthUserInfo, OAuthProvider oAuthProvider) {
        User authenticatedUser = Optional.ofNullable(userEmailService.findByEmail(oAuthUserInfo.getEmail())).map(UserEmail::getUser).orElse(null);
        boolean isNewUser = authenticatedUser == null;

        if (!isNewUser) {
            handleExistingUserOAuthLogin(authenticatedUser, oAuthUserInfo, oAuthProvider);
        } else {
            authenticatedUser = createUserFromOAuth(oAuthUserInfo);
        }

        return Map.of(
                "user", authenticatedUser,
                "isNewUser", isNewUser
        );
    }

    @Transactional
    public void handleExistingUserOAuthLogin(User authenticatedUser, OAuthUserInfoDTO oAuthUserInfo, OAuthProvider oAuthProvider) {
        OAuthConnection oAuthConnection = oAuthConnectionService.getConnectionByUserAndEmailOrThrow(authenticatedUser, oAuthUserInfo.getEmail(), oAuthProvider);
        if (oAuthConnection.isExpiringSoon() || oAuthConnection.isRevoked()) {
            oAuthConnectionService.updateAccessCredentials(oAuthConnection, oAuthUserInfo.getAccessCredentials());
        }
        handleOAuthEmailMatching(authenticatedUser, oAuthUserInfo);
    }

    @Transactional
    public User createUserFromOAuth(OAuthUserInfoDTO oAuthUserInfo) {
        User authenticatedUser = new User();
        authenticatedUser.setFirstname(oAuthUserInfo.getFirstname());
        authenticatedUser.setLastname(oAuthUserInfo.getLastname());
        authenticatedUser.setProfilePicture(oAuthUserInfo.getProfilePicture());
        authenticatedUser.setVerified(true);
        userRepository.save(authenticatedUser);

        UserEmail oAuthUserEmail = userEmailService.create(authenticatedUser, oAuthUserInfo.getEmail());
        authenticatedUser.setPrimaryEmail(oAuthUserEmail);
        userRepository.save(authenticatedUser);

        userPreferenceService.create(authenticatedUser, UserPreferenceOption.WORKLOGS_MONTHLY_TARGET_HOURS, "200");

        return authenticatedUser;
    }

    @Transactional
    public void handleOAuthEmailMatching(User authenticatedUser, OAuthUserInfoDTO oAuthUserInfo) {
        if (!authenticatedUser.isVerified() && authenticatedUser.getPrimaryEmail().getEmail().equals(oAuthUserInfo.getEmail())) {
            verifyUser(authenticatedUser);
            securityTokenService.deleteNonExpired(authenticatedUser, SecurityToken.Type.ACCOUNT_ACTIVATION);
        } else if (authenticatedUser.getPendingEmail() != null && authenticatedUser.getPendingEmail().getEmail().equals(oAuthUserInfo.getEmail())) {
            verifyAndChangePrimaryEmail(authenticatedUser, oAuthUserInfo.getEmail());
            securityTokenService.deleteNonExpired(authenticatedUser, SecurityToken.Type.NEW_EMAIL_VERIFICATION);
        }
    }

    public void verifyUser(User user) {
        user.setVerified(true);
        userRepository.save(user);
    }
    //</editor-fold>

    //<editor-fold desc="Password Management">
    @Transactional
    public void changePassword(PasswordDTO passwordDTO) {
        User user = AppConfig.getAuthenticatedCurrentUser();
        validateAndUpdateUserPassword(user, passwordDTO, false);
        sendPasswordFlowEmail(user, false);
    }

    public void validateAndUpdateUserPassword(User user, PasswordDTO passwordDTO, boolean isResetPassword) {
        if (user.isPasswordSet()) {
            if (!isResetPassword && (StringUtils.isEmpty(passwordDTO.getCurrentPassword()) || !passwordEncoder.matches(passwordDTO.getCurrentPassword(), user.getPassword()))) {
                throw new BusinessException("Current password is incorrect.");
            }
            if (passwordEncoder.matches(passwordDTO.getNewPassword(), user.getPassword())) {
                throw new BusinessException("New password cannot be the same as the current password");
            }
        }
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmNewPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        userRepository.save(user);
    }

    public void sendPasswordFlowEmail(User user, boolean isForReset) {
        SecurityTokenBuilder securityTokenBuilder = SecurityTokenBuilder.builder()
                .user(user)
                .targetEmail(user.getPrimaryEmail().getEmail())
                .type(isForReset ? SecurityToken.Type.PASSWORD_RESET : SecurityToken.Type.PASSWORD_CHANGE)
                .templateName(EmailTemplates.VERIFICATION_MAIL_TEMPLATE)
                .extraParameters(EmailTemplateUtil.getTemplateParams(isForReset ? SecurityToken.Type.PASSWORD_RESET : SecurityToken.Type.PASSWORD_CHANGE))
                .pageUrl("/change-password")
                .build();
        securityTokenService.createAndSend(securityTokenBuilder);
    }
    //</editor-fold>

    //<editor-fold desc="Email Management">
    @Transactional
    public void requestEmailChange(String email) {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        userEmailService.ensureValidEmailFormat(email);

        UserEmail existingUserEmail = userEmailService.findByEmail(email);
        if (existingUserEmail != null) {
            userEmailService.ensureEmailAvailableForUser(loggedUser, existingUserEmail);
        }
        securityTokenService.deleteNonExpired(loggedUser, SecurityToken.Type.NEW_EMAIL_VERIFICATION);

        UserEmail oldPendingEmail = loggedUser.getPendingEmail();
        if (oldPendingEmail != null) {
            loggedUser.setPendingEmail(null);
            userEmailService.deleteIfUnused(oldPendingEmail);
        }

        loggedUser.setPendingEmail(existingUserEmail != null ? existingUserEmail : userEmailService.create(loggedUser, email));
        userRepository.save(loggedUser);

        sendEmailVerificationSecurityToken(loggedUser, email);
    }

    @Transactional
    public void verifyAndChangePrimaryEmail(User user, String email) {
        userEmailService.ensureValidEmailFormat(email);
        UserEmail pendingEmail = user.getPendingEmail();
        if (pendingEmail == null || !pendingEmail.getEmail().equals(email)) {
            throw new BusinessException("No matching pending email found for verification");
        }

        UserEmail currentPrimary = user.getPrimaryEmail();
        user.setPrimaryEmail(pendingEmail);
        user.setPendingEmail(null);
        userRepository.save(user);

        userEmailService.deleteIfUnused(currentPrimary);

        EmailRequest emailRequest = EmailRequest.builder()
                .targetEmail(email)
                .subject("Email Changed")
                .templateName(EmailTemplates.INFO_MAIL_TEMPLATE)
                .parameters(Map.of("content", "Your account's email has been changed to " + email + ". If you did not perform this action, please contact support immediately."))
                .build();
        emailService.send(emailRequest);
    }

    public void sendEmailVerification() {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        if (loggedUser.getPendingEmail() == null) {
            throw new BusinessException("No email change awaiting verification");
        }
        sendEmailVerificationSecurityToken(loggedUser, loggedUser.getPendingEmail().getEmail());
    }

    public void sendEmailVerificationSecurityToken(User user, String email) {
        SecurityTokenBuilder securityTokenBuilder = SecurityTokenBuilder.builder()
                .user(user)
                .targetEmail(email)
                .type(SecurityToken.Type.NEW_EMAIL_VERIFICATION)
                .additionalInfo(email)
                .templateName(EmailTemplates.VERIFICATION_MAIL_TEMPLATE)
                .extraParameters(EmailTemplateUtil.getTemplateParams(SecurityToken.Type.NEW_EMAIL_VERIFICATION))
                .build();
        securityTokenService.createAndSend(securityTokenBuilder);
    }

    @Transactional
    public void removePendingEmail() {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        if (loggedUser.getPendingEmail() == null) {
            throw new BusinessException("No pending email to remove");
        }

        UserEmail pendingEmail = loggedUser.getPendingEmail();
        loggedUser.setPendingEmail(null);
        userRepository.save(loggedUser);

        userEmailService.deleteIfUnused(pendingEmail);
        securityTokenService.deleteNonExpired(loggedUser, SecurityToken.Type.NEW_EMAIL_VERIFICATION);
    }
    //</editor-fold>
}
