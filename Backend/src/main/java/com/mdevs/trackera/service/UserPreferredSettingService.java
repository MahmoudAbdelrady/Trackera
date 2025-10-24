package com.mdevs.trackera.service;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserPreferredSetting;
import com.mdevs.trackera.repository.UserPreferredSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserPreferredSettingService {
    private final UserPreferredSettingRepository userPreferredSettingRepository;

    @Autowired
    public UserPreferredSettingService(UserPreferredSettingRepository userPreferredSettingRepository) {
        this.userPreferredSettingRepository = userPreferredSettingRepository;
    }

    public void create(User user, String key, String value) {
        UserPreferredSetting preferredSetting = new UserPreferredSetting(user, key, value);
        userPreferredSettingRepository.save(preferredSetting);
    }

    public void deleteByUserAndKey(User user, String key) {
        userPreferredSettingRepository.deleteByUserAndKey(user, key);
    }
}
