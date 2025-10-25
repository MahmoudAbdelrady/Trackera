package com.mdevs.trackera.utils;

import java.util.Map;

public class EmailTemplateUtil {
    public static Map<String, String> accountActivationTemplateParams() {
        return Map.of(
                "emailTypeDesc", "Please click the link below to activate your account.",
                "linkLabel", "Activate my account"
        );
    }

    public static Map<String, String> resetPasswordTemplateParams() {
        return Map.of(
                "emailTypeDesc", "Please click the link below to reset your password.",
                "linkLabel", "Reset my password"
        );
    }
}
