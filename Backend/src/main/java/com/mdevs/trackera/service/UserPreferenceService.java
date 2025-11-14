package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.user.UserPreferenceDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserPreference;
import com.mdevs.trackera.repository.UserPreferenceRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.utils.AppUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserPreferenceService {
    private final UserPreferenceRepository userPreferenceRepository;

    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
    }

    //<editor-fold desc="Retrieval">
    public List<Map<String, Object>> getAllByUser(User user) {
        return userPreferenceRepository.findAllByUser(user).stream().map(setting -> {
            Map<String, Object> settingInfo = new HashMap<>();
            settingInfo.put("key", setting.getKey());
            settingInfo.put("value", setting.getValue());
            return settingInfo;
        }).toList();
    }

    public <T> T getPreferenceValue(User user, String key, Class<T> valueType) {
        UserPreference setting = userPreferenceRepository.findByUserAndKey(user, key);
        if (setting == null) {
            return null;
        }

        String value = setting.getValue();

        return valueType == String.class ? valueType.cast(value) : AppUtils.convertJsonStringToObject(value, valueType);
    }
    //</editor-fold>

    //<editor-fold desc="Creation and Update">
    public void create(User user, String key, String value) {
        UserPreference preference = new UserPreference(user, key, value);
        userPreferenceRepository.save(preference);
    }

    @Transactional
    public void updateAll(User user, List<UserPreferenceDTO> userPreferenceDTOList) {
        for (UserPreferenceDTO dto : userPreferenceDTOList) {
            UserPreference existingPreference = userPreferenceRepository.findByUserAndKey(user, dto.getKey());
            if (existingPreference != null) {
                existingPreference.setValue(dto.getValue());
                userPreferenceRepository.save(existingPreference);
            } else {
                create(user, dto.getKey(), dto.getValue());
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Deletion">
    public void deleteByUserAndKey(User user, String key) {
        userPreferenceRepository.deleteByUserAndKey(user, key);
    }
    //</editor-fold>

    //<editor-fold desc="Validations">
    public void validatePreference(UserPreferenceDTO preferenceDTO) {
        if (StringUtils.isEmpty(preferenceDTO.getKey())) {
            throw new BusinessException("Preference key is required");
        }
        if (StringUtils.isEmpty(preferenceDTO.getValue())) {
            throw new BusinessException("Preference value is required");
        }
    }
    //</editor-fold>
}
