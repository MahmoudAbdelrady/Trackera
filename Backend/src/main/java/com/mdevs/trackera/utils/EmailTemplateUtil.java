package com.mdevs.trackera.utils;

import com.mdevs.trackera.entity.SecurityToken;

import java.util.Map;

public class EmailTemplateUtil {
    public static Map<String, String> getTemplateParams(SecurityToken.Type type) {
        return switch (type) {
            case ACCOUNT_ACTIVATION -> Map.of(
                    "emailTypeDesc", "Please click the link below to activate your account.",
                    "linkLabel", "Activate my account"
            );
            case PASSWORD_RESET -> Map.of(
                    "emailTypeDesc", "Please click the link below to reset your password.",
                    "linkLabel", "Reset my password"
            );
            case PASSWORD_CHANGE -> Map.of(
                    "emailTypeDesc", "Your password has been changed. If you did not perform this action, please reset your password immediately.",
                    "linkLabel", "Reset my password"
            );
            case NEW_EMAIL_VERIFICATION -> Map.of(
                    "emailTypeDesc", "Please verify your new email address by clicking the link below:",
                    "linkLabel", "Verify my email"
            );
        };
    }
}
