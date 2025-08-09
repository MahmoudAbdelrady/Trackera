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
        Long tokenId = Long.parseLong(tokenPayload.get("tokenId"));
        SecurityToken securityRequestToken = securityTokenRepository.findOne(tokenId);
        if (securityRequestToken == null || securityRequestToken.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(MAX_SECURITY_TOKEN_MINUTES))) {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        return securityRequestToken;
    }

    @Transactional
    public void createAndSendSecurityToken(User user, SecurityToken.Type type, String additionalInfo, Map<String, String> extraParameters, String pageUrl, String templateName) {
        boolean notValid = false;
        if (securityTokenRepository.existsByUserAndTypeAndCreatedAtGreaterThanEqual(user, type, LocalDateTime.now().minusMinutes(MAX_SECURITY_TOKEN_MINUTES))) {
            if (!type.equals(SecurityToken.Type.PASSWORD_RESET)) {
                throw new BusinessException(type.getLabel() + " request has already been made recently. Please check your email or try again later.");
            }
            notValid = true;
        }

        if (notValid)
            return;

        SecurityToken securityToken = new SecurityToken(user, type);
        if (!StringUtils.isEmpty(additionalInfo)) {
            securityToken.setAdditionalInfo(additionalInfo);
        }
        securityTokenRepository.save(securityToken);
        String token = trackeraHasher.hashForSecurityToken(securityToken.getId());

        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("emailType", type.getLabel());
        templateParameters.put("verificationLink", AppConfig.getFrontendUrl() + (pageUrl != null ? pageUrl : "/security-verification") + "?token=" + token);
        if (extraParameters != null && !extraParameters.isEmpty()) {
            templateParameters.putAll(extraParameters);
        }
        TrackeraEmailTarget.builder()
                .targetEmail(user.getEmail())
                .subject(type.getLabel())
                .templateName(templateName)
                .parameters(templateParameters)
                .build().send();
    }

    public void deleteSecurityToken(SecurityToken securityToken) {
        securityTokenRepository.delete(securityToken);
    }
}
