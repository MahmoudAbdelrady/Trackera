package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserPreference;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPreferenceRepository extends BaseRepository<UserPreference> {
    UserPreference findByUserAndKey(User user, String key);

    void deleteByUserAndKey(User user, String key);

    List<UserPreference> findAllByUser(User user);
}
