package com.mdevs.trackera.service;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserInvalidToken;
import com.mdevs.trackera.repository.UserInvalidTokenRepository;
import com.mdevs.trackera.utils.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserInvalidTokenService {
    private final UserInvalidTokenRepository userInvalidTokenRepository;

    public void create(User user, String token, Date expirationDate, boolean isAccessToken) {
        LocalDateTime tokenExpiryDate = DateTimeUtil.convertDateToLocalDateTime(expirationDate);
        UserInvalidToken invalidAccessToken = new UserInvalidToken(user, DigestUtils.sha256Hex(token), tokenExpiryDate, isAccessToken);
        userInvalidTokenRepository.save(invalidAccessToken);
    }

    public boolean isInvalid(String userUuid, String token, boolean isAccessToken) {
        return userInvalidTokenRepository.existsByUserUuidAndTokenFingerprintAndIsAccessToken(userUuid, DigestUtils.sha256Hex(token), isAccessToken);
    }

    @Transactional
    public long deleteExpiredWithBatch(long maxId, int pageSize) {
        List<Long> invalidTokensIds = userInvalidTokenRepository.findExpiredAfterIdOrderById(LocalDateTime.now(), maxId, Pageable.ofSize(pageSize));
        if (!invalidTokensIds.isEmpty()) {
            userInvalidTokenRepository.deleteAllByIdInBatch(invalidTokensIds);
            return invalidTokensIds.getLast();
        }
        return -1;
    }
}
