package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface WorkLogRepository extends BaseRepository<WorkLog> {
    @Query("SELECT COUNT(w) > 0 FROM WorkLog w WHERE w.user = :user AND w.workDate = :workDate AND (:workLogId IS NULL OR w.id != :workLogId)")
    boolean existsByUserAndWorkDateAndWorkLogNot(@Param("user") User user, @Param("workDate") LocalDate workDate, @Param("workLogId") Long workLogId);

    @Query("SELECT COALESCE(SUM(w.totalMinutes), 0) FROM WorkLog w WHERE w.user = :user AND w.workDate BETWEEN :startDate AND :endDate")
    Integer sumTotalMinutesByUserAndWorkDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    WorkLog findByUserAndUuid(User user, String uuid);

    @Query("SELECT CASE WHEN SUM(CASE WHEN wld.status = 'SYNC_IN_PROGRESS' THEN 1 ELSE 0 END) > 0 THEN 'SYNC_IN_PROGRESS' " +
            "WHEN SUM(CASE WHEN wld.status = 'UNSYNC_IN_PROGRESS' THEN 1 ELSE 0 END) > 0 THEN 'UNSYNC_IN_PROGRESS' " +
            "WHEN SUM(CASE WHEN wld.status = 'NOT_SYNCED' THEN 1 ELSE 0 END) = 0 THEN 'SYNCED' " +
            "WHEN SUM(CASE WHEN wld.status = 'SYNCED' THEN 1 ELSE 0 END) = 0 THEN 'NOT_SYNCED' " +
            "ELSE 'PARTIALLY' END " +
            "FROM WorkLog w JOIN WorkLogDetail wld on wld.workLog = w WHERE w = :worklog")
    WorkLogStatus calculateWorkLogStatus(@Param("worklog") WorkLog worklog);
}
