package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends BaseRepository<User> {
    User findByUuid(String uuid);

    @Query("SELECT u FROM User u WHERE u.primaryEmail.email = :email")
    User findByPrimaryEmail(@Param("email") String email);
}
