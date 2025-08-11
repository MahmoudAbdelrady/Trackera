package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.UserInvalidToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface UserInvalidTokenRepository extends BaseRepository<UserInvalidToken> {
    @Query("SELECT t FROM UserInvalidToken t WHERE t.expiryDate <= :now AND t.id > :maxId ORDER BY t.id")
    List<UserInvalidToken> findAllByExpiryDateOrderById(@Param("now") Date now, @Param("maxId") Long maxId, Pageable pageable);

    @Query("SELECT t FROM UserInvalidToken t WHERE t.user.email = :email AND t.type = :tokenType AND t.id > :maxId ORDER BY t.id")
    List<UserInvalidToken> findAllByUserAndTokenTypeOrderById(@Param("email") String email, @Param("tokenType") UserInvalidToken.Type type, @Param("maxId") Long maxId, Pageable pageable);
}
