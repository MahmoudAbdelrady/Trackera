package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.WorkLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface WorkLogRepository extends BaseRepository<WorkLog> {
    @Query("SELECT COUNT(w) > 0 FROM WorkLog w WHERE w.user = :user AND w.workDate = :workDate AND (:workLogId IS NULL OR w.id != :workLogId)")
    boolean existsByUserAndWorkDateAndWorkLogNot(@Param("user") User user, @Param("workDate") LocalDate workDate, @Param("workLogId") Long workLogId);

    @Query("SELECT COALESCE(SUM(w.totalHours), 0) FROM WorkLog w WHERE w.user = :user AND w.workDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalHoursByUserAndWorkDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    WorkLog findByUserAndUuid(User user, String uuid);
}
