package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.user.UserPreferenceDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserPreferredSetting;
import com.mdevs.trackera.repository.UserPreferredSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserPreferredSettingService {
    private final UserPreferredSettingRepository userPreferredSettingRepository;

    @Autowired
    public UserPreferredSettingService(UserPreferredSettingRepository userPreferredSettingRepository) {
        this.userPreferredSettingRepository = userPreferredSettingRepository;
    }

    public List<Map<String, String>> getAllByUser(User user) {
        return userPreferredSettingRepository.findAllByUser(user).stream()
                .map(setting -> Map.of("key", setting.getKey(), "value", setting.getValue())).toList();
    }

    public void create(User user, String key, String value) {
        UserPreferredSetting preferredSetting = new UserPreferredSetting(user, key, value);
        userPreferredSettingRepository.save(preferredSetting);
    }

    @Transactional
    public void updateAll(User user, List<UserPreferenceDTO> userPreferenceDTOList) {
        for (UserPreferenceDTO dto : userPreferenceDTOList) {
            UserPreferredSetting existingSetting = userPreferredSettingRepository.findByUserAndKey(user, dto.getKey());
            if (existingSetting != null) {
                existingSetting.setValue(dto.getValue());
                userPreferredSettingRepository.save(existingSetting);
            } else {
                create(user, dto.getKey(), dto.getValue());
            }
        }
    }

    public void deleteByUserAndKey(User user, String key) {
        userPreferredSettingRepository.deleteByUserAndKey(user, key);
    }
}
