package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import com.mdevs.trackera.entity.UserOAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOAuthProviderRepository extends BaseRepository<UserOAuthProvider> {
    UserOAuthProvider findByUserAndProvider(User user, OAuthProvider provider);

    @Query("SELECT uop FROM UserOAuthProvider uop WHERE uop.user = :user AND uop.providerEmail.email = :email AND uop.provider = :provider")
    UserOAuthProvider findByUserAndEmailAndProvider(@Param("user") User user, @Param("email") String email, @Param("provider") OAuthProvider provider);

    List<UserOAuthProvider> findByUser(User user);

    int countByUser(User user);

    @Query("SELECT CASE WHEN NOT EXISTS (SELECT uop FROM UserOAuthProvider uop WHERE uop.user = :user AND uop.providerEmail = :userEmail) THEN true ELSE false END")
    boolean notExistsByUserAndProviderEmail(@Param("user") User user, @Param("userEmail") UserEmail userEmail);
}
