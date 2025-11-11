package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.LoggedUserDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.dto.auth.SignUpDTO;
import com.mdevs.trackera.dto.user.UserPreferenceDTO;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.repository.UserEmailRepository;
import com.mdevs.trackera.repository.UserOAuthProviderRepository;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.EmailTemplates;
import com.mdevs.trackera.shared.SecurityTokenBuilder;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.TrackeraEmailTarget;
import com.mdevs.trackera.utils.EmailTemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    private final UserEmailRepository userEmailRepository;

    private final UserOAuthProviderRepository userOAuthProviderRepository;

    private final UserEmailService userEmailService;

    private final UserOAuthProviderService userOAuthProviderService;

    private final UserPreferredSettingService userPreferredSettingService;

    private final JiraService jiraService;

    private final SecurityTokenService securityTokenService;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$";

    public UserService(UserRepository userRepository, UserEmailRepository userEmailRepository, UserOAuthProviderRepository userOAuthProviderRepository, UserEmailService userEmailService,
                       UserOAuthProviderService userOAuthProviderService, UserPreferredSettingService userPreferredSettingService, JiraService jiraService, SecurityTokenService securityTokenService,
                       ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userEmailRepository = userEmailRepository;
        this.userOAuthProviderRepository = userOAuthProviderRepository;
        this.userEmailService = userEmailService;
        this.userOAuthProviderService = userOAuthProviderService;
        this.userPreferredSettingService = userPreferredSettingService;
        this.jiraService = jiraService;
        this.securityTokenService = securityTokenService;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return Optional.ofNullable(userRepository.findByPrimaryEmail(email)).orElseThrow(() -> new UsernameNotFoundException("Account not found."));
    }

    public LoggedUserDTO getMeInfo() {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        LoggedUserDTO loggedUserDTO = modelMapper.map(AppConfig.getAuthenticatedCurrentUser(), LoggedUserDTO.class);
        loggedUserDTO.setPrimaryEmail(loggedUser.getPrimaryEmail().getEmail());
        if (loggedUser.getPendingEmail() != null) {
            loggedUserDTO.setPendingEmail(loggedUser.getPendingEmail().getEmail());
        }
        return loggedUserDTO;
    }

    public List<Map<String, Object>> getUserOAuthProviders() {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        Map<OAuthProvider, UserOAuthProvider> linkedProviders = userOAuthProviderRepository.findByUser(loggedUser).stream().collect(Collectors.toMap(UserOAuthProvider::getProvider, o -> o));

        return Arrays.stream(OAuthProvider.values()).map(provider -> {
            UserOAuthProvider userProvider = linkedProviders.get(provider);
            boolean isLinked = userProvider != null;
            Map<String, Object> providerInfo = new HashMap<>();
            providerInfo.put("provider", Map.of("name", provider.getDisplayName(), "code", provider.getCode()));
            providerInfo.put("isLinked", isLinked);
            if (isLinked) {
                providerInfo.put("email", userProvider.getProviderEmail().getEmail());
                providerInfo.put("isRevoked", userProvider.isRevoked());
            }
            return providerInfo;
        }).toList();
    }

    @Transactional
    public User create(SignUpDTO signUpDTO) {
        if (userEmailRepository.existsByEmail(signUpDTO.getEmail())) {
            throw new BusinessException("Email already in use");
        }
        if (!signUpDTO.getPassword().equals(signUpDTO.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        User user = modelMapper.map(signUpDTO, User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        UserEmail userEmail = userEmailService.create(user, signUpDTO.getEmail());
        user.setPrimaryEmail(userEmail);
        return userRepository.save(user);
    }

    @Transactional
    public Map<String, Object> createOrGetOAuthUser(OAuthUserInfoDTO oAuthUserInfo, OAuthProvider oAuthProvider) {
        User authenticatedUser = Optional.ofNullable(userEmailRepository.findByEmail(oAuthUserInfo.getEmail())).map(UserEmail::getUser).orElse(null);
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
        UserOAuthProvider userOAuthProvider = userOAuthProviderService.getUserLinkedProviderOrThrow(authenticatedUser, oAuthUserInfo.getEmail(), oAuthProvider);
        if (userOAuthProvider.isExpired() || userOAuthProvider.isRevoked()) {
            userOAuthProviderService.updateAccessCredentials(userOAuthProvider, oAuthUserInfo.getAccessCredentials());
        }
        handleOAuthEmailMatching(oAuthUserInfo, authenticatedUser);
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
        return authenticatedUser;
    }

    @Transactional
    public void handleOAuthEmailMatching(OAuthUserInfoDTO oAuthUserInfo, User authenticatedUser) {
        if (!authenticatedUser.isVerified() && authenticatedUser.getPrimaryEmail().getEmail().equals(oAuthUserInfo.getEmail())) {
            verifyUser(authenticatedUser);
            securityTokenService.deleteNonExpiredSecurityToken(authenticatedUser, SecurityToken.Type.ACCOUNT_ACTIVATION);
        } else if (authenticatedUser.getPendingEmail() != null && authenticatedUser.getPendingEmail().getEmail().equals(oAuthUserInfo.getEmail())) {
            verifyAndChangePrimaryEmail(authenticatedUser, oAuthUserInfo.getEmail());
            securityTokenService.deleteNonExpiredSecurityToken(authenticatedUser, SecurityToken.Type.NEW_EMAIL_VERIFICATION);
        }
    }

    public void verifyUser(User user) {
        user.setVerified(true);
        userRepository.save(user);
    }

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
        securityTokenService.createAndSendSecurityToken(securityTokenBuilder);
    }

    @Transactional
    public void requestEmailChange(String email) {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        validateUserEmail(email);

        UserEmail existingUserEmail = userEmailRepository.findByEmail(email);
        if (existingUserEmail != null) {
            validateEmailAvailability(loggedUser, existingUserEmail);
        }
        securityTokenService.deleteNonExpiredSecurityToken(loggedUser, SecurityToken.Type.NEW_EMAIL_VERIFICATION);

        UserEmail oldPendingEmail = loggedUser.getPendingEmail();
        if (oldPendingEmail != null) {
            loggedUser.setPendingEmail(null);
            userEmailService.deleteIfUnused(oldPendingEmail);
        }

        loggedUser.setPendingEmail(existingUserEmail != null ? existingUserEmail : userEmailService.create(loggedUser, email));
        userRepository.save(loggedUser);

        sendEmailVerificationSecurityToken(loggedUser, email);
    }

    private void validateEmailAvailability(User loggedUser, UserEmail existingUserEmail) {
        if (!existingUserEmail.getUser().getId().equals(loggedUser.getId())) {
            throw new BusinessException("Email is already in use");
        }

        boolean isPrimaryOrPending = loggedUser.getPrimaryEmail().getId().equals(existingUserEmail.getId()) || (loggedUser.getPendingEmail() != null && loggedUser.getPendingEmail().getId().equals(existingUserEmail.getId()));
        if (isPrimaryOrPending) {
            throw new BusinessException("Email is already associated with your account");
        }
    }

    @Transactional
    public void verifyAndChangePrimaryEmail(User user, String email) {
        validateUserEmail(email);
        UserEmail pendingEmail = user.getPendingEmail();
        if (pendingEmail == null || !pendingEmail.getEmail().equals(email)) {
            throw new BusinessException("No matching pending email found for verification");
        }

        UserEmail currentPrimary = user.getPrimaryEmail();
        user.setPrimaryEmail(pendingEmail);
        user.setPendingEmail(null);
        userRepository.save(user);

        userEmailService.deleteIfUnused(currentPrimary);

        TrackeraEmailTarget.builder()
                .targetEmail(currentPrimary.getEmail())
                .subject("Email Changed")
                .templateName(EmailTemplates.INFO_MAIL_TEMPLATE)
                .parameters(Map.of("content", "Your account's email has been changed to " + email + ". If you did not perform this action, please contact support immediately."))
                .build().send();
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
        securityTokenService.createAndSendSecurityToken(securityTokenBuilder);
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
        securityTokenService.deleteNonExpiredSecurityToken(loggedUser, SecurityToken.Type.NEW_EMAIL_VERIFICATION);
    }

    public void validateUserEmail(String email) {
        if (StringUtils.isEmpty(email) || !email.matches(EMAIL_REGEX)) {
            throw new BusinessException("Invalid email format");
        }
    }

    public List<Map<String, Object>> getUserPreferences() {
        List<Map<String, Object>> preferences = userPreferredSettingService.getAllByUser(AppConfig.getAuthenticatedCurrentUser());
        for (Map<String, Object> preference : preferences) {
            if (preference.get("key").equals(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY)) {
                jiraService.loadJiraPreference(preference);
            }
        }
        return preferences;
    }

    public void updateUserPreferences(List<UserPreferenceDTO> userPreferenceDTOList) {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        for (UserPreferenceDTO preferenceDTO : userPreferenceDTOList) {
            userPreferredSettingService.validatePreference(preferenceDTO);
            if (preferenceDTO.getKey().equals(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY)) {
                jiraService.handleJiraPreference(preferenceDTO, loggedUser);
            }
        }
        userPreferredSettingService.updateAll(loggedUser, userPreferenceDTOList);
    }
}
