package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserPreferredSetting;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPreferredSettingRepository extends BaseRepository<UserPreferredSetting> {
    UserPreferredSetting findByUserAndKey(User user, String key);

    void deleteByUserAndKey(User user, String key);

    List<UserPreferredSetting> findAllByUser(User user);
}
