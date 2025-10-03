package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOAuthProviderRepository extends BaseRepository<UserOAuthProvider> {
    boolean existsByUserAndProvider(User user, OAuthProvider provider);

    UserOAuthProvider findByUserAndProvider(User user, OAuthProvider provider);

    List<UserOAuthProvider> findByUser(User user);

    int countByUser(User user);
}
