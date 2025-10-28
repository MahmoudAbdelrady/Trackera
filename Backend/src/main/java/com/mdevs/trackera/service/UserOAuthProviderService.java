package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.repository.UserOAuthProviderRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.utils.TrackeraHasher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserOAuthProviderService {
    private final UserOAuthProviderRepository userOAuthProviderRepository;

    private final UserPreferredSettingService userPreferredSettingService;

    private final TrackeraHasher trackeraHasher;

    public UserOAuthProviderService(UserOAuthProviderRepository userOAuthProviderRepository, UserPreferredSettingService userPreferredSettingService, TrackeraHasher trackeraHasher) {
        this.userOAuthProviderRepository = userOAuthProviderRepository;
        this.userPreferredSettingService = userPreferredSettingService;
        this.trackeraHasher = trackeraHasher;
    }

    public void createOrUpdate(User user, OAuthUserInfoDTO oAuthUserInfoDTO, OAuthProvider oAuthProvider, UserOAuthProvider existingUserOAuthProvider) {
        if (existingUserOAuthProvider == null) {
            existingUserOAuthProvider = new UserOAuthProvider(user, oAuthProvider);
            existingUserOAuthProvider.setEmail(oAuthUserInfoDTO.getEmail());
        }
        updateAccessCredentials(existingUserOAuthProvider, oAuthUserInfoDTO.getAccessCredentials());
    }

    public void updateAccessCredentials(UserOAuthProvider existingUserOAuthProvider, OAuthAccessCredentialsDTO oAuthAccessCredentialsDTO) {
        existingUserOAuthProvider.setAccessToken(trackeraHasher.encryptToBase64(oAuthAccessCredentialsDTO.getAccessToken(), false));
        existingUserOAuthProvider.setRefreshToken(trackeraHasher.encryptToBase64(oAuthAccessCredentialsDTO.getRefreshToken(), false));
        existingUserOAuthProvider.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(oAuthAccessCredentialsDTO.getExpiresIn()));
        existingUserOAuthProvider.setRevoked(false);
        userOAuthProviderRepository.save(existingUserOAuthProvider);
    }

    public void ensureUserDoesNotHaveActiveLinkedProvider(User user, OAuthProvider oAuthProvider) {
        UserOAuthProvider userOAuthProvider = userOAuthProviderRepository.findByUserAndProvider(user, oAuthProvider);
        if (userOAuthProvider != null && !userOAuthProvider.isRevoked()) {
            throw new BusinessException("The current account is already linked with " + oAuthProvider.getDisplayName());
        }
    }

    public void ensureUserHasActiveLinkedProvider(User user, OAuthProvider provider) {
        UserOAuthProvider linked = userOAuthProviderRepository.findByUserAndProvider(user, provider);
        if (linked == null || linked.isRevoked()) {
            throw new BusinessException("This account is not linked with " + provider.getDisplayName());
        }
    }

    public UserOAuthProvider delete(User user, OAuthProvider oAuthProvider) {
        UserOAuthProvider deletedOAuthProvider = userOAuthProviderRepository.findByUserAndProvider(user, oAuthProvider);
        if (deletedOAuthProvider == null) {
            throw new BusinessException("Your account is not linked with " + oAuthProvider.getDisplayName());
        }
        if (userOAuthProviderRepository.countByUser(user) <= 1 && !user.isPasswordSet()) {
            throw new BusinessException("You must have at least one sign-in method linked to your account");
        }
        userOAuthProviderRepository.delete(deletedOAuthProvider);
        if (oAuthProvider.equals(OAuthProvider.JIRA)) {
            userPreferredSettingService.deleteByUserAndKey(user, JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY);
        }
        return deletedOAuthProvider;
    }
}
