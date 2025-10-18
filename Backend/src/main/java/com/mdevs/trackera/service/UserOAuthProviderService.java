package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.repository.UserOAuthProviderRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.utils.TrackeraHasher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserOAuthProviderService {
    private final UserOAuthProviderRepository userOAuthProviderRepository;

    private final UserPreferredSettingService userPreferredSettingService;

    private final TrackeraHasher trackeraHasher;

    @Autowired
    public UserOAuthProviderService(UserOAuthProviderRepository userOAuthProviderRepository, UserPreferredSettingService userPreferredSettingService, TrackeraHasher trackeraHasher) {
        this.userOAuthProviderRepository = userOAuthProviderRepository;
        this.userPreferredSettingService = userPreferredSettingService;
        this.trackeraHasher = trackeraHasher;
    }

    public void createOrUpdate(User user, OAuthUserInfoDTO oAuthUserInfoDTO, OAuthProvider oAuthProvider, UserOAuthProvider existingUserOAuthProvider) {
        if (existingUserOAuthProvider == null) {
            existingUserOAuthProvider = new UserOAuthProvider(user, oAuthProvider);
        }
        existingUserOAuthProvider.setEmail(oAuthUserInfoDTO.getEmail());
        existingUserOAuthProvider.setAccessToken(trackeraHasher.encryptToBase64(oAuthUserInfoDTO.getAccessCredentials().getAccessToken(), false));
        existingUserOAuthProvider.setRefreshToken(trackeraHasher.encryptToBase64(oAuthUserInfoDTO.getAccessCredentials().getRefreshToken(), false));
        existingUserOAuthProvider.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(oAuthUserInfoDTO.getAccessCredentials().getExpiresIn()));
        existingUserOAuthProvider.setRevoked(false);
        userOAuthProviderRepository.save(existingUserOAuthProvider);
    }

    public UserOAuthProvider getByUserAndProvider(User user, OAuthProvider oAuthProvider) {
        return userOAuthProviderRepository.findByUserAndProvider(user, oAuthProvider);
    }

    public UserOAuthProvider delete(User user, OAuthProvider oAuthProvider) {
        UserOAuthProvider userOAuthProvider = userOAuthProviderRepository.findByUserAndProvider(user, oAuthProvider);
        if (userOAuthProvider == null) {
            throw new BusinessException("Your account is not linked with " + oAuthProvider.getDisplayName());
        }
        if (userOAuthProviderRepository.countByUser(user) <= 1 && !user.isPasswordSet()) {
            throw new BusinessException("You must have at least one sign-in method linked to your account");
        }
        userOAuthProviderRepository.delete(userOAuthProvider);
        if (oAuthProvider.equals(OAuthProvider.JIRA)) {
            userPreferredSettingService.deleteByUserAndKey(user, JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY);
        }
        return userOAuthProvider;
    }
}
