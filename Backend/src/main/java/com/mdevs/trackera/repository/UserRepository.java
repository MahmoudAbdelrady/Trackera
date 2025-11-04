package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends BaseRepository<User> {
    User findByUuid(String uuid);
}
