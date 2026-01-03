package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.UserInvalidToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserInvalidTokenRepository extends BaseRepository<UserInvalidToken> {
    @Query("SELECT t FROM UserInvalidToken t WHERE t.expiryDate <= :now AND t.id > :maxId ORDER BY t.id")
    List<UserInvalidToken> findExpiredAfterIdOrderById(@Param("now") LocalDateTime now, @Param("maxId") Long maxId, Pageable pageable);

    @Query("SELECT t FROM UserInvalidToken t WHERE t.user.uuid = :uuid AND t.isAccessToken = :isAccessToken AND t.id > :maxId ORDER BY t.id")
    List<UserInvalidToken> findByUserUuidAndIsAccessTokenAfterIdOrderById(@Param("uuid") String uuid, @Param("isAccessToken") boolean isAccessToken, @Param("maxId") Long maxId, Pageable pageable);

    @Query("SELECT CASE WHEN EXISTS (SELECT 1 FROM UserInvalidToken t WHERE t.user.uuid = :uuid AND t.tokenFingerprint = :tokenFingerprint AND t.isAccessToken = :isAccessToken) THEN true ELSE false END")
    boolean existsByUserUuidAndTokenFingerprintAndIsAccessToken(String uuid, String tokenFingerprint, boolean isAccessToken);
}
