package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.repository.UserOAuthProviderRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthProviderFactory;
import com.mdevs.trackera.utils.TrackeraHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserOAuthProviderService {
    private final UserOAuthProviderRepository userOAuthProviderRepository;

    private final UserPreferredSettingService userPreferredSettingService;

    private final OAuthProviderFactory oAuthProviderFactory;

    private final TrackeraHasher trackeraHasher;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserOAuthProviderService.class);

    public UserOAuthProviderService(UserOAuthProviderRepository userOAuthProviderRepository, UserPreferredSettingService userPreferredSettingService, OAuthProviderFactory oAuthProviderFactory, TrackeraHasher trackeraHasher) {
        this.userOAuthProviderRepository = userOAuthProviderRepository;
        this.userPreferredSettingService = userPreferredSettingService;
        this.oAuthProviderFactory = oAuthProviderFactory;
        this.trackeraHasher = trackeraHasher;
    }

    public void createOrUpdate(User user, OAuthUserInfoDTO oAuthUserInfoDTO, OAuthProvider oAuthProvider, UserOAuthProvider existingUserOAuthProvider) {
        if (existingUserOAuthProvider == null) {
            existingUserOAuthProvider = new UserOAuthProvider(user, oAuthProvider);
            existingUserOAuthProvider.setEmail(oAuthUserInfoDTO.getEmail());
        }
        updateAccessCredentials(existingUserOAuthProvider, oAuthUserInfoDTO.getAccessCredentials());
    }

    public String resolveValidAccessToken(UserOAuthProvider userOAuthProvider) {
        try {
            if (userOAuthProvider.isExpired()) {
                userOAuthProvider = refreshAndUpdateCredentials(userOAuthProvider);
                if (userOAuthProvider.isRevoked()) {
                    throw new RuntimeException("Failed to refresh access token");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while resolving access token for provider {}: {}", userOAuthProvider.getProvider(), e.getMessage(), e);
            throw new RuntimeException("Failed to authenticate with " + userOAuthProvider.getProvider());
        }

        return trackeraHasher.decryptFromBase64(userOAuthProvider.getAccessToken(), false);
    }


    @Transactional
    public UserOAuthProvider refreshAndUpdateCredentials(UserOAuthProvider userOAuthProvider) {
        try {
            OAuthAccessCredentialsDTO newCredentials = oAuthProviderFactory.getProvider(userOAuthProvider.getProvider()).refreshOAuthProviderCredentials(userOAuthProvider);
            userOAuthProvider = updateAccessCredentials(userOAuthProvider, newCredentials);
        } catch (Exception e) {
            userOAuthProvider.setRevoked(true);
            userOAuthProvider = userOAuthProviderRepository.save(userOAuthProvider);
        }
        return userOAuthProvider;
    }


    public UserOAuthProvider updateAccessCredentials(UserOAuthProvider existingUserOAuthProvider, OAuthAccessCredentialsDTO oAuthAccessCredentialsDTO) {
        existingUserOAuthProvider.setAccessToken(trackeraHasher.encryptToBase64(oAuthAccessCredentialsDTO.getAccessToken(), false));
        existingUserOAuthProvider.setRefreshToken(trackeraHasher.encryptToBase64(oAuthAccessCredentialsDTO.getRefreshToken(), false));
        existingUserOAuthProvider.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(oAuthAccessCredentialsDTO.getExpiresIn()));
        existingUserOAuthProvider.setRevoked(false);
        return userOAuthProviderRepository.save(existingUserOAuthProvider);
    }

    public void ensureUserHasActiveLinkedProvider(User user, OAuthProvider provider) {
        UserOAuthProvider linked = userOAuthProviderRepository.findByUserAndProvider(user, provider);
        if (linked == null || linked.isRevoked()) {
            throw new BusinessException("This account is not linked with " + provider.getDisplayName());
        }
    }

    public void ensureUserDoesNotHaveActiveLinkedProvider(User user, OAuthProvider oAuthProvider) {
        UserOAuthProvider userOAuthProvider = userOAuthProviderRepository.findByUserAndProvider(user, oAuthProvider);
        if (userOAuthProvider != null && !userOAuthProvider.isRevoked()) {
            throw new BusinessException("The current account is already linked with " + oAuthProvider.getDisplayName());
        }
    }

    public UserOAuthProvider validateAndGetOAuthProvider(User user, OAuthProvider provider) {
        UserOAuthProvider userOAuthProvider = userOAuthProviderRepository.findByUserAndProvider(user, provider);
        if (userOAuthProvider == null) {
            throw new BusinessException(provider + " account not linked");
        }
        if (userOAuthProvider.isRevoked()) {
            throw new BusinessException(provider + " account link has been revoked. Please relink your account.");
        }
        return userOAuthProvider;
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
