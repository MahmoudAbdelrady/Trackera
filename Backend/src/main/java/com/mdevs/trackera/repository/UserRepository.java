package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends BaseRepository<User> {
    User findByUuid(String uuid);

    User findByEmail(String email);

    @Query("SELECT CASE WHEN EXISTS (SELECT 1 FROM User u LEFT JOIN u.oAuthProviders p WHERE (:id IS NULL OR u.id != :id) AND (u.email = :email OR u.pendingEmail = :email OR p.providerEmail = :email)) THEN true ELSE false END")
    boolean existsByUserEmailOrOAuthProvidersEmailAndIdNot(@Param("email") String email, @Param("id") Long id);
}
