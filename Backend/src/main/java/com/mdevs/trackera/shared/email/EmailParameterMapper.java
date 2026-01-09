package com.mdevs.trackera.shared.email;

import com.mdevs.trackera.entity.SecurityToken;

import java.util.Map;

public class EmailParameterMapper {
    public static Map<String, String> getSecurityTemplateParams(SecurityToken.Type type) {
        return switch (type) {
            case ACCOUNT_ACTIVATION -> Map.of(
                    EmailTemplateParams.EMAIL_TYPE_DESC, "Please click the link below to activate your account.",
                    EmailTemplateParams.LINK_LABEL, "Activate my account"
            );
            case PASSWORD_RESET -> Map.of(
                    EmailTemplateParams.EMAIL_TYPE_DESC, "Please click the link below to reset your password.",
                    EmailTemplateParams.LINK_LABEL, "Reset my password"
            );
            case PASSWORD_CHANGE -> Map.of(
                    EmailTemplateParams.EMAIL_TYPE_DESC, "Your password has been changed. If you did not perform this action, please reset your password immediately.",
                    EmailTemplateParams.LINK_LABEL, "Reset my password"
            );
            case NEW_EMAIL_VERIFICATION -> Map.of(
                    EmailTemplateParams.EMAIL_TYPE_DESC, "Please verify your new email address by clicking the link below:",
                    EmailTemplateParams.LINK_LABEL, "Verify my email"
            );
        };
    }
}
