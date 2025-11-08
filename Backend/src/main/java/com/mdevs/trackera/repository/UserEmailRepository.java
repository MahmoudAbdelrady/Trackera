package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEmailRepository extends BaseRepository<UserEmail> {
    @Query("SELECT CASE WHEN EXISTS (SELECT ue FROM UserEmail ue WHERE ue.email = :email) THEN true ELSE false END")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN EXISTS (SELECT ue FROM UserEmail ue WHERE ue.email = :email AND ue.user != :user) THEN true ELSE false END")
    boolean existsByEmailAndUserNot(@Param("email") String email, @Param("user") User user);

    UserEmail findTop1ByEmailOrderByCreatedAtDesc(String email);

    @Query("SELECT ue FROM UserEmail ue WHERE ue.user.primaryEmail = ue AND ue.email = :email")
    UserEmail findPrimaryEmail(@Param("email") String email);

    @Query("SELECT ue FROM UserEmail ue WHERE ue.user = :user AND ue.email = :email AND ue.verified = false")
    UserEmail findUnverifiedByUserAndEmail(@Param("user") User user, @Param("email") String email);
}
