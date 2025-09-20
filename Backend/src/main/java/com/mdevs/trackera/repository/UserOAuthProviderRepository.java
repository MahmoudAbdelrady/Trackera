package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOAuthProviderRepository extends BaseRepository<UserOAuthProvider> {
    boolean existsByUserAndProvider(User user, OAuthProvider provider);

    UserOAuthProvider findByUserAndProvider(User user, OAuthProvider provider);
}
