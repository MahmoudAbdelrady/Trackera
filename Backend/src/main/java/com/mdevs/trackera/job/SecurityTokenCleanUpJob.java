package com.mdevs.trackera.job;

import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.repository.SecurityTokenRepository;
import com.mdevs.trackera.service.SecurityTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class SecurityTokenCleanUpJob {
    private final SecurityTokenRepository securityTokenRepository;

    private final SecurityTokenCleanUpJob selfRef;

    public SecurityTokenCleanUpJob(SecurityTokenRepository securityTokenRepository, @Lazy SecurityTokenCleanUpJob securityTokenCleanUpJob) {
        this.securityTokenRepository = securityTokenRepository;
        this.selfRef = securityTokenCleanUpJob;
    }

    @Scheduled(cron = "0 0 0 ? * *")
    public void cleanUpExpiredTokens() {
        long maxId = 0;
        int pageSize = 100;
        List<SecurityToken> securityTokens;
        LocalDateTime timeOut = LocalDateTime.now().minusMinutes(SecurityTokenService.MAX_SECURITY_TOKEN_MINUTES);
        do {
            securityTokens = securityTokenRepository.findSecurityRequestTokenWithCreationDateLessThanEqual(timeOut, maxId, PageRequest.of(0, pageSize));
            if (!securityTokens.isEmpty()) {
                try {
                    selfRef.deleteExpiredTokens(securityTokens);
                } catch (Exception e) {
                    log.error("Error while deleting expired security request tokens in batch: {} Error:\n{}", maxId, e.getMessage());
                }
                maxId = securityTokens.getLast().getId();
            }
        } while (!securityTokens.isEmpty());
    }

    @Transactional
    public void deleteExpiredTokens(List<SecurityToken> securityTokens) {
        securityTokenRepository.deleteAllInBatch(securityTokens);
    }
}
