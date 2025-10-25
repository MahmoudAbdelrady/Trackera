package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.user.UserPreferenceDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserPreferredSetting;
import com.mdevs.trackera.repository.UserPreferredSettingRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserPreferredSettingService {
    private final UserPreferredSettingRepository userPreferredSettingRepository;

    @Autowired
    public UserPreferredSettingService(UserPreferredSettingRepository userPreferredSettingRepository) {
        this.userPreferredSettingRepository = userPreferredSettingRepository;
    }

    public List<Map<String, Object>> getAllByUser(User user) {
        return userPreferredSettingRepository.findAllByUser(user).stream().map(setting -> {
            Map<String, Object> settingInfo = new HashMap<>();
            settingInfo.put("key", setting.getKey());
            settingInfo.put("value", setting.getValue());
            return settingInfo;
        }).toList();
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

    public void validatePreference(UserPreferenceDTO preferenceDTO) {
        if (StringUtils.isEmpty(preferenceDTO.getKey())) {
            throw new BusinessException("Preference key is required");
        }
        if (StringUtils.isEmpty(preferenceDTO.getValue())) {
            throw new BusinessException("Preference value is required");
        }
    }
}
