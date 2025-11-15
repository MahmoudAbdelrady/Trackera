package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserPreference;
import com.mdevs.trackera.repository.UserPreferenceRepository;
import com.mdevs.trackera.shared.enums.UserPreferenceOption;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.utils.AppUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserPreferenceService {
    private final UserPreferenceRepository userPreferenceRepository;

    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
    }

    //<editor-fold desc="Retrieval">
    public Map<String, Object> getAll() {
        Map<String, Object> preferencesMap = new HashMap<>();
        userPreferenceRepository.findAllByUser(AppConfig.getAuthenticatedCurrentUser()).forEach(pref -> {
            UserPreferenceOption option = pref.getOption();
            preferencesMap.put(option.getCode(), AppUtils.convertValue(pref.getValue(), option.getValueType()));
        });
        return preferencesMap;
    }

    public Object getPreferenceValue(User user, UserPreferenceOption option) {
        UserPreference preference = userPreferenceRepository.findByUserAndOption(user, option);
        if (preference == null) {
            return null;
        }

        String value = preference.getValue();

        return AppUtils.convertValue(value, option.getValueType());
    }
    //</editor-fold>

    //<editor-fold desc="Creation and Update">
    public void create(User user, UserPreferenceOption option, String value) {
        UserPreference preference = new UserPreference(user, option, value);
        userPreferenceRepository.save(preference);
    }

    public void createOrUpdate(User user, UserPreferenceOption option, String value) {
        UserPreference existingPreference = userPreferenceRepository.findByUserAndOption(user, option);
        if (existingPreference != null) {
            existingPreference.setValue(value);
            userPreferenceRepository.save(existingPreference);
        } else {
            create(user, option, value);
        }
    }

    @Transactional
    public void updateAll(User user, Map<String, Object> updatedPreferences) {
        for (Map.Entry<String, Object> preference : updatedPreferences.entrySet()) {
            UserPreferenceOption option = UserPreferenceOption.fromCode(preference.getKey());
            createOrUpdate(user, option, preference.getValue().toString());
        }
    }
    //</editor-fold>

    //<editor-fold desc="Deletion">
    public void deleteByUserAndOption(User user, UserPreferenceOption option) {
        userPreferenceRepository.deleteByUserAndOption(user, option);
    }
    //</editor-fold>

    //<editor-fold desc="Validations">
    public void validatePreference(Map.Entry<String, Object> preference) {
        if (StringUtils.isEmpty(preference.getKey())) {
            throw new BusinessException("Preference key is required");
        }
        if (preference.getValue() == null || StringUtils.isEmpty(preference.getValue().toString())) {
            throw new BusinessException("Preference value is required");
        }
    }
    //</editor-fold>
}
