package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.email.EmailRequest;
import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.repository.SecurityTokenRepository;
import com.mdevs.trackera.shared.EmailService;
import com.mdevs.trackera.shared.SecurityTokenBuilder;
import com.mdevs.trackera.shared.exceptions.types.UnauthorizedException;
import com.mdevs.trackera.utils.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SecurityTokenService {
    private final SecurityTokenRepository securityTokenRepository;

    private final EmailService emailService;

    private final CryptoUtil cryptoUtil;

    public static final long MAX_SECURITY_TOKEN_MINUTES = 15;

    //<editor-fold desc="Retrieval and Consumption">
    public SecurityToken validateAndGet(String token) {
        Map<String, String> tokenPayload = cryptoUtil.parseSecurityToken(token);
        if (tokenPayload == null || tokenPayload.isEmpty()) {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        Long tokenId = Long.parseLong(tokenPayload.get("tokenId"));
        SecurityToken securityToken = securityTokenRepository.findOne(tokenId);
        if (securityToken == null || securityToken.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(MAX_SECURITY_TOKEN_MINUTES))) {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        return securityToken;
    }

    @Transactional
    public SecurityToken validateAndConsume(String token) {
        SecurityToken securityToken = validateAndGet(token);
        securityTokenRepository.delete(securityToken);
        return securityToken;
    }
    //</editor-fold>

    //<editor-fold desc="Creation and Update">
    @Transactional
    public void createAndSend(SecurityTokenBuilder builder) {
        if (hasRecentActivationToken(builder.getUser(), builder.getType())) {
            return;
        }

        SecurityToken securityToken = new SecurityToken(builder.getUser(), builder.getType());
        if (!StringUtils.isEmpty(builder.getAdditionalInfo())) {
            securityToken.setAdditionalInfo(builder.getAdditionalInfo());
        }
        securityTokenRepository.save(securityToken);
        String token = cryptoUtil.hashForSecurityToken(securityToken.getId());

        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("emailType", builder.getType().getLabel());
        templateParameters.put("verificationLink", AppConfig.getFrontendUrl() + (builder.getPageUrl() != null ? builder.getPageUrl() : "/security-verification") + "?token=" + token);
        if (builder.getExtraParameters() != null && !builder.getExtraParameters().isEmpty()) {
            templateParameters.putAll(builder.getExtraParameters());
        }

        EmailRequest emailRequest = EmailRequest.builder()
                .targetEmail(builder.getTargetEmail())
                .subject(builder.getType().getLabel())
                .templateName(builder.getTemplateName())
                .parameters(templateParameters)
                .build();
        emailService.send(emailRequest);
    }
    //</editor-fold>

    //<editor-fold desc="Deletion">
    public void deleteNonExpired(User user, SecurityToken.Type type) {
        securityTokenRepository.deleteByUserAndTypeAndCreatedAtGreaterThanEqual(user, type, LocalDateTime.now().minusMinutes(MAX_SECURITY_TOKEN_MINUTES));
    }

    @Transactional
    public long deleteExpiredTokensBatch(long maxId, int pageSize) {
        List<SecurityToken> securityTokens = securityTokenRepository.findSecurityRequestTokenWithCreationDateLessThanEqual(LocalDateTime.now().minusMinutes(SecurityTokenService.MAX_SECURITY_TOKEN_MINUTES), maxId, Pageable.ofSize(pageSize));
        if (!securityTokens.isEmpty()) {
            securityTokenRepository.deleteAllInBatch(securityTokens);
            return securityTokens.getLast().getId();
        }
        return -1;
    }
    //</editor-fold>

    //<editor-fold desc="Validations">
    public boolean hasRecentActivationToken(User user, SecurityToken.Type type) {
        return securityTokenRepository.existsByUserAndTypeAndCreatedAtGreaterThanEqual(user, type, LocalDateTime.now().minusMinutes(MAX_SECURITY_TOKEN_MINUTES));
    }
    //</editor-fold>
}
