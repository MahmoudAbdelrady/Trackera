package com.mdevs.trackera.service;

import com.mdevs.trackera.config.AppConfig;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.repository.SecurityTokenRepository;
import com.mdevs.trackera.shared.exception.BusinessException;
import com.mdevs.trackera.shared.exception.UnauthorizedException;
import com.mdevs.trackera.shared.utils.TrackeraHasher;
import com.mdevs.trackera.shared.utils.mail.TrackeraEmailTarget;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SecurityTokenService {
    private final SecurityTokenRepository securityTokenRepository;

    private final TrackeraHasher trackeraHasher;

    public final static long MAX_SECURITY_TOKEN_MINUTES = 15;

    @Autowired
    public SecurityTokenService(SecurityTokenRepository securityTokenRepository, TrackeraHasher trackeraHasher) {
        this.securityTokenRepository = securityTokenRepository;
        this.trackeraHasher = trackeraHasher;
    }

    public SecurityToken getSecurityToken(String token) {
        Map<String, String> tokenPayload = trackeraHasher.parseSecurityToken(token);
        if (tokenPayload == null || tokenPayload.isEmpty()) {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        Long userId = Long.parseLong(tokenPayload.get("userId"));
        SecurityToken.Type securityRequestType = SecurityToken.Type.valueOf(tokenPayload.get("type"));
        SecurityToken securityRequestToken = securityTokenRepository.findByUserIdAndTypeAndCreatedAtGreaterThanEqual(userId, securityRequestType, LocalDateTime.now().minusMinutes(MAX_SECURITY_TOKEN_MINUTES));
        if (securityRequestToken == null || !trackeraHasher.isMatch(token, securityRequestToken.getToken(), true)) {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        return securityRequestToken;
    }

    @Transactional
    public void createAndSendSecurityToken(User user, SecurityToken.Type type, String additionalInfo, Map<String, String> extraParameters, String pageUrl, String emailSubject, String templateName) {
        if (securityTokenRepository.existsByUserAndTypeAndCreatedAtGreaterThanEqual(user, type, LocalDateTime.now().minusMinutes(MAX_SECURITY_TOKEN_MINUTES))) {
            throw new BusinessException(type.getLabel() + " request has already been made recently. Please check your email or try again later.");
        }

        SecurityToken securityToken = new SecurityToken(user, type);
        if (!StringUtils.isEmpty(additionalInfo)) {
            securityToken.setAdditionalInfo(additionalInfo);
        }
        String actualToken = securityToken.getToken();
        securityToken.setToken(trackeraHasher.hash(actualToken, true));
        securityToken = securityTokenRepository.save(securityToken);

        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("verificationLink", AppConfig.getFrontendUrl() + (pageUrl != null ? pageUrl : "/security-verification") + "?token=" + actualToken);
        if (extraParameters != null && !extraParameters.isEmpty()) {
            templateParameters.putAll(extraParameters);
        }
        TrackeraEmailTarget.builder()
                .targetEmail(user.getEmail())
                .subject(emailSubject)
                .templateName(templateName)
                .parameters(templateParameters)
                .build().send();
    }

    public void deleteSecurityToken(SecurityToken securityToken) {
        securityTokenRepository.delete(securityToken);
    }
}
