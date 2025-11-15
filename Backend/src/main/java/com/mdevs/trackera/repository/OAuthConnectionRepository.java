package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import com.mdevs.trackera.entity.OAuthConnection;
import com.mdevs.trackera.oauth.OAuthProvider;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OAuthConnectionRepository extends BaseRepository<OAuthConnection> {
    OAuthConnection findByUserAndProvider(User user, OAuthProvider provider);

    @Query("SELECT oac FROM OAuthConnection oac WHERE oac.user = :user AND oac.accountEmail.email = :email AND oac.provider = :provider")
    OAuthConnection findByUserAndEmailAndProvider(@Param("user") User user, @Param("email") String email, @Param("provider") OAuthProvider provider);

    List<OAuthConnection> findByUser(User user);

    int countByUser(User user);

    @Query("SELECT CASE WHEN NOT EXISTS (SELECT oac FROM OAuthConnection oac WHERE oac.user = :user AND oac.accountEmail = :userEmail) THEN true ELSE false END")
    boolean notExistsByUserAndProviderEmail(@Param("user") User user, @Param("userEmail") UserEmail userEmail);
}
