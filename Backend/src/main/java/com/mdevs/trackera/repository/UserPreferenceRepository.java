package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserPreference;
import com.mdevs.trackera.shared.enums.UserPreferenceOption;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPreferenceRepository extends BaseRepository<UserPreference> {
    UserPreference findByUserAndOption(User user, UserPreferenceOption option);

    void deleteByUserAndOption(User user, UserPreferenceOption option);

    List<UserPreference> findAllByUser(User user);
}
