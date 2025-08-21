package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.SecurityToken;
import com.mdevs.trackera.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityTokenRepository extends BaseRepository<SecurityToken> {
    boolean existsByUserAndTypeAndCreatedAtGreaterThanEqual(User user, SecurityToken.Type type, LocalDateTime timeOut);

    @Query("SELECT s FROM SecurityToken s WHERE s.createdAt <= :timeOut AND s.id > :maxId ORDER BY s.id")
    List<SecurityToken> findSecurityRequestTokenWithCreationDateLessThanEqual(@Param("timeOut") LocalDateTime timeOut, @Param("maxId") Long maxId, Pageable pageable);
}
