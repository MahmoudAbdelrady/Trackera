package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.LoggedUserDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.PasswordDTO;
import com.mdevs.trackera.dto.auth.SignUpDTO;
import com.mdevs.trackera.dto.jira.AccessibleResourceDTO;
import com.mdevs.trackera.dto.user.UserEmailDTO;
import com.mdevs.trackera.dto.user.UserPreferenceDTO;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.repository.UserEmailRepository;
import com.mdevs.trackera.repository.UserOAuthProviderRepository;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.EmailTemplates;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.exceptions.types.NotFoundException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.utils.AppUtils;
import com.mdevs.trackera.shared.TrackeraEmailTarget;
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

    private final UserOAuthProviderService userOAuthProviderService;

    private final UserPreferredSettingService userPreferredSettingService;

    private final JiraService jiraService;

    private final SecurityTokenService securityTokenService;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$";

    public UserService(UserRepository userRepository, UserEmailRepository userEmailRepository, UserOAuthProviderRepository userOAuthProviderRepository, UserOAuthProviderService userOAuthProviderService,
                       UserPreferredSettingService userPreferredSettingService, JiraService jiraService, SecurityTokenService securityTokenService, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userEmailRepository = userEmailRepository;
        this.userOAuthProviderRepository = userOAuthProviderRepository;
        this.userOAuthProviderService = userOAuthProviderService;
        this.userPreferredSettingService = userPreferredSettingService;
        this.jiraService = jiraService;
        this.securityTokenService = securityTokenService;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return Optional.ofNullable(userEmailRepository.findPrimaryEmail(email)).map(UserEmail::getUser).orElseThrow(() -> new UsernameNotFoundException("Account not found."));
    }

    public LoggedUserDTO getMeInfo() {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        LoggedUserDTO loggedUserDTO = modelMapper.map(AppConfig.getAuthenticatedCurrentUser(), LoggedUserDTO.class);
        loggedUserDTO.setPrimaryEmail(new UserEmailDTO(loggedUser.getPrimaryEmail()));
        if (loggedUser.getPendingEmail() != null) {
            loggedUserDTO.setPendingEmail(new UserEmailDTO(loggedUser.getPendingEmail()));
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

        UserEmail userEmail = createUserEmail(user, signUpDTO.getEmail(), false);
        user.setPrimaryEmail(userEmail);
        return userRepository.save(user);
    }

    public UserEmail createUserEmail(User user, String email, boolean isVerified) {
        UserEmail userEmail = new UserEmail(user, email, isVerified);
        return userEmailRepository.save(userEmail);
    }

    @Transactional
    public Map<String, Object> createOrGetOAuthUser(OAuthUserInfoDTO oAuthUserInfo, OAuthProvider oAuthProvider) {
        UserEmail userEmail = userEmailRepository.findTop1ByEmailOrderByCreatedAtDesc(oAuthUserInfo.getEmail());
        User authenticatedUser = Optional.ofNullable(userEmail).map(UserEmail::getUser).orElse(null);
        boolean isNewUser = authenticatedUser == null;

        if (authenticatedUser != null) {
            handleExistingUserOAuthLogin(oAuthUserInfo, oAuthProvider, authenticatedUser);
        } else {
            authenticatedUser = createUserFromOAuth(oAuthUserInfo);
        }

        return Map.of(
                "user", authenticatedUser,
                "isNewUser", isNewUser
        );
    }

    @Transactional
    public void handleExistingUserOAuthLogin(OAuthUserInfoDTO oAuthUserInfo, OAuthProvider oAuthProvider, User authenticatedUser) {
        UserOAuthProvider userOAuthProvider = userOAuthProviderService.getUserLinkedProviderOrThrow(authenticatedUser, oAuthUserInfo.getEmail(), oAuthProvider);
        if (userOAuthProvider.isExpired() || userOAuthProvider.isRevoked()) {
            userOAuthProviderService.updateAccessCredentials(userOAuthProvider, oAuthUserInfo.getAccessCredentials());
        }
        verifyEmailIfMatchesOAuth(oAuthUserInfo, authenticatedUser);
    }

    @Transactional
    public void verifyEmailIfMatchesOAuth(OAuthUserInfoDTO oAuthUserInfo, User authenticatedUser) {
        if (!authenticatedUser.getPrimaryEmail().isVerified() && authenticatedUser.getPrimaryEmail().getEmail().equals(oAuthUserInfo.getEmail())) {
            UserEmail unVerifiedEmail = authenticatedUser.getPrimaryEmail();
            unVerifiedEmail.setVerified(true);
            userEmailRepository.save(unVerifiedEmail);
            securityTokenService.deleteNonExpiredSecurityToken(authenticatedUser, SecurityToken.Type.ACCOUNT_ACTIVATION);
        } else if (authenticatedUser.getPendingEmail() != null && authenticatedUser.getPendingEmail().getEmail().equals(oAuthUserInfo.getEmail())) {
            verifyAndChangePrimaryEmail(authenticatedUser, oAuthUserInfo.getEmail());
            securityTokenService.deleteNonExpiredSecurityToken(authenticatedUser, SecurityToken.Type.NEW_EMAIL_VERIFICATION);
        }
    }

    @Transactional
    public User createUserFromOAuth(OAuthUserInfoDTO oAuthUserInfo) {
        User authenticatedUser;
        authenticatedUser = new User();
        authenticatedUser.setFirstname(oAuthUserInfo.getFirstname());
        authenticatedUser.setLastname(oAuthUserInfo.getLastname());
        authenticatedUser.setProfilePicture(oAuthUserInfo.getProfilePicture());
        authenticatedUser.setVerified(true);
        userRepository.save(authenticatedUser);

        UserEmail oAuthUserEmail = createUserEmail(authenticatedUser, oAuthUserInfo.getEmail(), true);
        authenticatedUser.setPrimaryEmail(oAuthUserEmail);
        userRepository.save(authenticatedUser);
        return authenticatedUser;
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
        String description = isForReset
                ? "Please click the link below to reset your password."
                : "Your password has been changed. If you did not perform this action, please reset your password immediately.";

        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("emailTypeDesc", description);
        templateParameters.put("linkLabel", "Reset my password");
        securityTokenService.createAndSendSecurityToken(user, user.getPrimaryEmail().getEmail(), isForReset ? SecurityToken.Type.PASSWORD_RESET : SecurityToken.Type.PASSWORD_CHANGE, null, templateParameters, "/change-password", EmailTemplates.VERIFICATION_MAIL_TEMPLATE);
    }

    @Transactional
    public void changeEmail(String email) {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        validateUserEmail(email);

        UserEmail existingUserEmail = userEmailRepository.findTop1ByEmailOrderByCreatedAtDesc(email);
        if (existingUserEmail != null) {
            throw new BusinessException(existingUserEmail.getUser().getId().equals(loggedUser.getId()) ? "Email is already associated with your account" : "Email is already in use");
        }
        securityTokenService.deleteNonExpiredSecurityToken(loggedUser, SecurityToken.Type.NEW_EMAIL_VERIFICATION);

        UserEmail oldPendingEmail = loggedUser.getPendingEmail();
        if (oldPendingEmail != null) {
            loggedUser.setPendingEmail(null);
            userEmailRepository.delete(oldPendingEmail);
        }

        UserEmail newEmail = createUserEmail(loggedUser, email, false);
        loggedUser.setPendingEmail(newEmail);
        userRepository.save(loggedUser);

        sendEmailVerificationSecurityToken(loggedUser, email);
    }

    @Transactional
    public void verifyAndChangePrimaryEmail(User user, String email) {
        validateUserEmail(email);
        UserEmail pendingEmail = user.getPendingEmail();
        if (pendingEmail == null || !pendingEmail.getEmail().equals(email)) {
            throw new BusinessException("Verification link expired due to a new email change");
        }

        UserEmail currentPrimary = user.getPrimaryEmail();

        pendingEmail.setVerified(true);
        userEmailRepository.save(pendingEmail);

        user.setPrimaryEmail(pendingEmail);
        user.setPendingEmail(null);
        userRepository.save(user);

        userEmailRepository.delete(currentPrimary);

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
        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("emailTypeDesc", "Please verify your new email address by clicking the link below:");
        templateParameters.put("linkLabel", "Verify my email");
        securityTokenService.createAndSendSecurityToken(user, email, SecurityToken.Type.NEW_EMAIL_VERIFICATION, email, templateParameters, null, EmailTemplates.VERIFICATION_MAIL_TEMPLATE);
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
        userEmailRepository.delete(pendingEmail);
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
                Map<String, Object> jiraProjectInfo = AppUtils.convertJsonStringToObject(preference.get("value").toString(), Map.class);
                preference.put("value", jiraProjectInfo);
            }
        }
        return preferences;
    }

    public void updateUserPreferences(List<UserPreferenceDTO> userPreferenceDTOList) {
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        for (UserPreferenceDTO preferenceDTO : userPreferenceDTOList) {
            userPreferredSettingService.validatePreference(preferenceDTO);
            if (preferenceDTO.getKey().equals(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY)) {
                AccessibleResourceDTO accessibleResourceDTO = jiraService.getUserSites(loggedUser).stream().filter(site -> site.getId().equals(preferenceDTO.getValue()))
                        .findFirst().orElseThrow(() -> new NotFoundException("Site not found"));

                preferenceDTO.setValue(AppUtils.convertObjectToJsonString(accessibleResourceDTO));
            }
        }
        userPreferredSettingService.updateAll(loggedUser, userPreferenceDTOList);
    }
}
