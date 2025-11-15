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

    UserEmail findByUserAndEmail(User user, String email);

    UserEmail findByEmail(String email);
}
