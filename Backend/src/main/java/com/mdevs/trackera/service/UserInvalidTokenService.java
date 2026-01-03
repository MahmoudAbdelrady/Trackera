package com.mdevs.trackera.service;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserInvalidToken;
import com.mdevs.trackera.repository.UserInvalidTokenRepository;
import com.mdevs.trackera.utils.AppUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class UserInvalidTokenService {
    private final UserInvalidTokenRepository userInvalidTokenRepository;

    public UserInvalidTokenService(UserInvalidTokenRepository userInvalidTokenRepository) {
        this.userInvalidTokenRepository = userInvalidTokenRepository;
    }

    public void create(User user, String token, Date expirationDate, boolean isAccessToken) {
        LocalDateTime tokenExpiryDate = AppUtils.convertDateToLocalDateTime(expirationDate);
        UserInvalidToken invalidAccessToken = new UserInvalidToken(user, DigestUtils.sha256Hex(token), tokenExpiryDate, isAccessToken);
        userInvalidTokenRepository.save(invalidAccessToken);
    }

    public boolean isTokenInvalid(String userUuid, String token, boolean isAccessToken) {
        return userInvalidTokenRepository.existsByUserUuidAndTokenFingerprintAndIsAccessToken(userUuid, DigestUtils.sha256Hex(token), isAccessToken);
    }

    @Transactional
    public long deleteExpiredTokensBatch(long maxId, int pageSize) {
        List<UserInvalidToken> invalidTokens = userInvalidTokenRepository.findExpiredAfterIdOrderById(LocalDateTime.now(), maxId, PageRequest.of(0, pageSize));
        if (!invalidTokens.isEmpty()) {
            userInvalidTokenRepository.deleteAllInBatch(invalidTokens);
            return invalidTokens.getLast().getId();
        }
        return -1;
    }
}
