package com.mdevs.trackera.job;

import com.mdevs.trackera.entity.UserInvalidToken;
import com.mdevs.trackera.repository.UserInvalidTokenRepository;
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
public class UserInvalidTokenCleanUpJob {
    private final UserInvalidTokenRepository userInvalidTokenRepository;

    private final UserInvalidTokenCleanUpJob selfRef;

    public UserInvalidTokenCleanUpJob(UserInvalidTokenRepository userInvalidTokenRepository, @Lazy UserInvalidTokenCleanUpJob selfRef) {
        this.userInvalidTokenRepository = userInvalidTokenRepository;
        this.selfRef = selfRef;
    }

    @Scheduled(cron = "0 0 0 ? * *")
    public void cleanUpExpiredTokens() {
        long maxId = 0;
        int pageSize = 100;
        List<UserInvalidToken> userInvalidTokens;
        LocalDateTime timeOut = LocalDateTime.now();
        do {
            userInvalidTokens = userInvalidTokenRepository.findAllByExpiryDateOrderById(timeOut, maxId, PageRequest.of(0, pageSize));
            if (!userInvalidTokens.isEmpty()) {
                try {
                    selfRef.deleteExpiredTokens(userInvalidTokens);
                } catch (Exception e) {
                    log.error("Error while deleting expired user invalid tokens in batch: {} Error:\n{}", maxId, e.getMessage());
                }
                maxId = userInvalidTokens.getLast().getId();
            }
        } while (!userInvalidTokens.isEmpty());
    }

    @Transactional
    public void deleteExpiredTokens(List<UserInvalidToken> userInvalidTokens) {
        userInvalidTokenRepository.deleteAllInBatch(userInvalidTokens);
    }
}
