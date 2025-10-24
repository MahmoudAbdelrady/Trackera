package com.mdevs.trackera.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.mdevs.trackera.shared.enums.EmailTag;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.exceptions.types.NotFoundException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.utils.AppUtils;
import com.mdevs.trackera.shared.utils.mail.TrackeraEmailTarget;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    @Autowired
    public UserService(UserRepository userRepository, UserEmailRepository userEmailRepository, UserOAuthProviderRepository userOAuthProviderRepository, UserOAuthProviderService userOAuthProviderService, UserPreferredSettingService userPreferredSettingService, JiraService jiraService,
                       SecurityTokenService securityTokenService, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
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
        try {
            return validateAndGetUserByEmail(email);
        } catch (Exception e) {
            throw new UsernameNotFoundException(e.getMessage());
        }
    }

    public User validateAndGetUserByEmail(String email) {
        UserEmail userEmail = userEmailRepository.findByEmail(email);
        if (userEmail == null) {
            throw new UsernameNotFoundException("Account not found. Please create an account and try again.");
        }
        if (!userEmail.isPrimary() && !userEmail.isVerified()) {
            throw new SecurityException("Invalid email or password.");
        }
        return userEmail.getUser();
    }

    public LoggedUserDTO getMeInfo() {
        return modelMapper.map(AppConfig.getCurrentUser(), LoggedUserDTO.class);
    }

    public List<Map<String, Object>> getUserOAuthProviders() {
        User loggedUser = AppConfig.getCurrentUser();
        List<UserOAuthProvider> userOAuthProviders = userOAuthProviderRepository.findByUser(loggedUser);
        return Arrays.stream(OAuthProvider.values()).map(p -> {
            UserOAuthProvider userOAuthProvider = userOAuthProviders.stream().filter(uop -> uop.getProvider().equals(p)).findFirst().orElse(null);
            Map<String, Object> providerInfo = new HashMap<>();
            providerInfo.put("provider", Map.of("code", p.getCode(), "name", p.getDisplayName()));
            boolean userOAuthProviderExists = userOAuthProvider != null;
            providerInfo.put("isLinked", userOAuthProviderExists);
            providerInfo.put("email", userOAuthProviderExists && !StringUtils.isEmpty(userOAuthProvider.getEmail()) ? userOAuthProvider.getEmail() : null);
            if (userOAuthProviderExists) {
                providerInfo.put("isRevoked", userOAuthProvider.isRevoked());
            }
            return providerInfo;
        }).toList();
    }

    public User create(SignUpDTO signUpDTO) {
        if (userEmailRepository.existsByEmail(signUpDTO.getEmail())) {
            throw new BusinessException("Email already exists");
        }
        if (!signUpDTO.getPassword().equals(signUpDTO.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }
        User user = modelMapper.map(signUpDTO, User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        UserEmail userEmail = new UserEmail(user, signUpDTO.getEmail(), true, false);
        userEmailRepository.save(userEmail);
        return user;
    }

    public void createUserEmail(User user, String email, boolean isPrimary, boolean isVerified, List<EmailTag> tags) {
        UserEmail userEmail = Optional.ofNullable(userEmailRepository.findByUserAndEmail(user, email)).orElse(new UserEmail(user, email, isPrimary, isVerified));
        userEmail.addTags(tags);
        if (tags.stream().anyMatch(EmailTag::isOAuthTag)) {
            userEmail.removeTag(EmailTag.PASSWORD_REQUIRED);
        }
        userEmailRepository.save(userEmail);
    }

    public Map<String, Object> createOrGetOAuthUser(OAuthUserInfoDTO oAuthUserInfo, OAuthProvider oAuthProvider) {
        User authenticatedUser = Optional.ofNullable(userEmailRepository.findByEmail(oAuthUserInfo.getEmail())).map(UserEmail::getUser).orElse(null);
        boolean createOAuthProvider = true;

        if (authenticatedUser != null) { // means the user is logging in with an oAuth provider
            createOAuthProvider = false;
            if (userOAuthProviderService.getByUserAndProvider(authenticatedUser, oAuthProvider) == null) {
                throw new BusinessException("This account is not linked with " + oAuthProvider.getDisplayName());
            }
        } else {
            authenticatedUser = new User();
            authenticatedUser.setEmail(oAuthUserInfo.getEmail());
            authenticatedUser.setFirstname(oAuthUserInfo.getFirstname());
            authenticatedUser.setLastname(oAuthUserInfo.getLastname());
            authenticatedUser.setProfilePicture(oAuthUserInfo.getProfilePicture());
            authenticatedUser.setVerified(true);
            userRepository.save(authenticatedUser);
        }

        return Map.of(
                "user", authenticatedUser,
                "createOAuthProvider", createOAuthProvider
        );
    }

    public void verifyUser(User user) {
        user.setVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public String changePassword(PasswordDTO passwordDTO) {
        User user = Objects.requireNonNull(AppConfig.getCurrentUser());
        if (!StringUtils.isEmpty(passwordDTO.getCurrentPassword()) && (!user.isPasswordSet() || !passwordEncoder.matches(passwordDTO.getCurrentPassword(), user.getPassword()))) {
            throw new BusinessException("Current password is incorrect.");
        }
        user.setPassword(getUserNewPassword(user, passwordDTO));
        sendResetPasswordEmail(user, user.getEmail(), "Your password has been changed. If you did not perform this action, please reset your password immediately.", false);
        userRepository.save(user);
        userEmailRepository.findAllByUser(user).forEach(ue -> {
            ue.removeTag(EmailTag.PASSWORD_REQUIRED);
            userEmailRepository.save(ue);
        });
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

    public void sendResetPasswordEmail(User user, String targetEmail, String description, boolean isForReset) {
        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("emailTypeDesc", description);
        templateParameters.put("linkLabel", "Reset my password");
        securityTokenService.createAndSendSecurityToken(user, targetEmail, isForReset ? SecurityToken.Type.PASSWORD_RESET : SecurityToken.Type.PASSWORD_CHANGE, null, templateParameters, "/change-password", EmailTemplates.VERIFICATION_MAIL_TEMPLATE);
    }

    public List<UserEmailDTO> getUserEmails() {
        User loggedUser = Objects.requireNonNull(AppConfig.getCurrentUser());
        List<UserEmail> userEmails = userEmailRepository.findAllByUserOrderByCreatedAt(loggedUser);
        return userEmails.stream().sorted(Comparator.comparing(UserEmail::isPrimary).reversed()).map(userEmail -> {
            UserEmailDTO userEmailDTO = modelMapper.map(userEmail, UserEmailDTO.class);
            userEmailDTO.setOAuthLinked(userEmail.getTags().stream().anyMatch(EmailTag::isOAuthTag));
            return  userEmailDTO;
        }).toList();
    }

    @Transactional
    public void addEmail(String email) {
        User loggedUser = Objects.requireNonNull(AppConfig.getCurrentUser());
        validateUserEmail(email);
        UserEmail existingUserEmail = userEmailRepository.findByEmail(email);
        if (existingUserEmail != null) {
            throw new BusinessException(existingUserEmail.getUser().getId().equals(loggedUser.getId()) ? "Email already exists" : "Email is already associated with another account");
        }
        createUserEmail(loggedUser, email, false, false, List.of());
        sendEmailVerificationSecurityToken(loggedUser, email);
    }

    public void verifyEmail(User user, String email) {
        validateUserEmail(email);
        UserEmail userEmail = Optional.ofNullable(userEmailRepository.findByUserAndEmail(user, email)).orElseThrow(() -> new NotFoundException("Email not found"));
        if (userEmail.isVerified()) {
            throw new BusinessException("Email is already verified");
        }
        userEmail.setVerified(true);
        if (!user.isPasswordSet()) {
            userEmail.addTag(EmailTag.PASSWORD_REQUIRED);
        }
        userEmailRepository.save(userEmail);
    }

    @Transactional
    public void makeEmailPrimary(String email) {
        User loggedUser = Objects.requireNonNull(AppConfig.getCurrentUser());
        validateUserEmail(email);
        UserEmail secondaryUserEmail = Optional.ofNullable(userEmailRepository.findByUserAndEmail(loggedUser, email)).orElseThrow(() -> new NotFoundException("Email not found"));
        if (!secondaryUserEmail.isVerified()) {
            throw new BusinessException("Cannot set unverified email as primary");
        }

        UserEmail currentPrimaryEmail = userEmailRepository.findByUserAndIsPrimaryTrue(loggedUser);
        if (currentPrimaryEmail.getEmail().equals(secondaryUserEmail.getEmail())) {
            throw new BusinessException("Email is already primary");
        }

        if (!loggedUser.isPasswordSet() && secondaryUserEmail.hasTag(EmailTag.PASSWORD_REQUIRED)) {
            throw new BusinessException("You need to set a password before making this email your primary address.");
        }

        currentPrimaryEmail.setPrimary(false);
        userEmailRepository.save(currentPrimaryEmail);

        secondaryUserEmail.setPrimary(true);
        loggedUser.setEmail(secondaryUserEmail.getEmail());
        userRepository.save(loggedUser);
        userEmailRepository.save(secondaryUserEmail);

        TrackeraEmailTarget.builder()
                .targetEmail(currentPrimaryEmail.getEmail())
                .subject("Primary Email Changed")
                .templateName(EmailTemplates.INFO_MAIL_TEMPLATE)
                .parameters(Map.of("content", "Your primary email has been changed to " + secondaryUserEmail.getEmail() + ". If you did not perform this action, please contact support immediately."))
                .build().send();
    }

    public void sendVerificationEmail(String email) {
        User loggedUser = Objects.requireNonNull(AppConfig.getCurrentUser());
        validateUserEmail(email);
        UserEmail userEmail = Optional.ofNullable(userEmailRepository.findByUserAndEmail(loggedUser, email)).orElseThrow(() -> new NotFoundException("Email not found"));
        if (userEmail.isVerified()) {
            throw new BusinessException("Email is already verified");
        }
        sendEmailVerificationSecurityToken(loggedUser, email);
    }

    public void sendEmailVerificationSecurityToken(User user, String email) {
        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("emailTypeDesc", "Please verify your new email address by clicking the link below:");
        templateParameters.put("linkLabel", "Verify my email");
        securityTokenService.createAndSendSecurityToken(user, email, SecurityToken.Type.NEW_EMAIL_VERIFICATION, email, templateParameters, null, EmailTemplates.VERIFICATION_MAIL_TEMPLATE);
    }

    @Transactional
    public void removeEmail(String email) {
        User loggedUser = Objects.requireNonNull(AppConfig.getCurrentUser());
        validateUserEmail(email);
        UserEmail userEmail = Optional.ofNullable(userEmailRepository.findByUserAndEmail(loggedUser, email)).orElseThrow(() -> new NotFoundException("Email not found"));
        if (userEmail.isPrimary()) {
            throw new BusinessException("Cannot remove primary email");
        }

        userEmailRepository.delete(userEmail);
        if (!userEmail.isVerified()) {
            securityTokenService.deleteNonExpiredSecurityToken(loggedUser, SecurityToken.Type.NEW_EMAIL_VERIFICATION);
        } else {
            Arrays.stream(OAuthProvider.values())
                    .filter(provider -> userEmail.getTags().contains(provider.getEmailTag()))
                    .forEach(provider -> userOAuthProviderService.delete(loggedUser, provider));
        }

        TrackeraEmailTarget.builder()
                .targetEmail(userEmail.getEmail())
                .subject("Email Removed")
                .templateName(EmailTemplates.INFO_MAIL_TEMPLATE)
                .parameters(Map.of("content", "The email " + userEmail.getEmail() + " has been removed from your account. If you did not perform this action, please contact support immediately."))
                .build().send();
    }

    public void removeEmailTag(User user, String email, EmailTag tag) {
        UserEmail userEmail = Optional.ofNullable(userEmailRepository.findByUserAndEmail(user, email)).orElseThrow(() -> new NotFoundException("Email not found"));
        userEmail.removeTag(tag);
        userEmailRepository.save(userEmail);
    }

    public void validateUserEmail(String email) {
        if (StringUtils.isEmpty(email) || !email.matches(EMAIL_REGEX)) {
            throw new BusinessException("Invalid email format");
        }
    }

    public List<Map<String, String>> getUserPreferences() {
        List<Map<String, String>> preferences = userPreferredSettingService.getAllByUser(AppConfig.getCurrentUser());
        for (Map<String, String> preference : preferences) {
            if (preference.get("key").equals(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY)) {
                try {
                    Map<String, String> jiraProjectInfo = AppUtils.getObjectMapper().readValue(preference.get("value"), Map.class);
                    preference.put("value", jiraProjectInfo.get("id"));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return preferences;
    }

    public void updateUserPreferences(List<UserPreferenceDTO> userPreferenceDTOList) {
        User loggedUser = Objects.requireNonNull(AppConfig.getCurrentUser());
        for (UserPreferenceDTO preferenceDTO : userPreferenceDTOList) {
            if (preferenceDTO.getKey().equals(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY)) {
                AccessibleResourceDTO accessibleResourceDTO = jiraService.getUserSites(loggedUser).stream().filter(site -> site.getId().equals(preferenceDTO.getValue()))
                        .findFirst().orElseThrow(() -> new NotFoundException("Site not found"));
                preferenceDTO.setValue(accessibleResourceDTO.getId());
            }
        }
        userPreferredSettingService.updateAll(loggedUser, userPreferenceDTOList);
    }
}
