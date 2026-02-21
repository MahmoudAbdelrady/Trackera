package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface WorkLogDetailRepository extends BaseRepository<WorkLogDetail> {
    WorkLogDetail findByUuid(String uuid);

    void deleteAllByWorkLog(WorkLog workLog);

    @Query("SELECT NEW MAP(wld.taskName as taskName, SUM(wld.duration) AS totalMinutes, " +
            "CASE WHEN SUM(CASE WHEN wld.status = 'SYNC_IN_PROGRESS' THEN 1 ELSE 0 END) > 0 THEN 'SYNC_IN_PROGRESS' " +
            "     WHEN SUM(CASE WHEN wld.status = 'UNSYNC_IN_PROGRESS' THEN 1 ELSE 0 END) > 0 THEN 'UNSYNC_IN_PROGRESS' " +
            "     WHEN SUM(CASE WHEN wld.status = 'IN_QUEUE' THEN 1 ELSE 0 END) > 0 THEN 'IN_QUEUE' " +
            "     WHEN SUM(CASE WHEN wld.status = 'NOT_SYNCED' THEN 1 ELSE 0 END) = 0 THEN 'SYNCED' " +
            "     WHEN SUM(CASE WHEN wld.status = 'SYNCED' THEN 1 ELSE 0 END) = 0 THEN 'NOT_SYNCED' " +
            "     ELSE 'PARTIALLY' END AS status," +
            "CASE WHEN SUM(CASE WHEN wld.syncError IS NOT NULL THEN 1 ELSE 0 END) > 0 THEN true ELSE false END AS hasError) " +
            "FROM WorkLogDetail wld WHERE wld.workLog = :workLog GROUP BY wld.taskName")
    List<Map<String, Object>> getGroupedWorkLogDetailsByWorkLog(WorkLog workLog);

    boolean existsByWorkLog(WorkLog workLog);

    boolean existsByWorkLogAndTaskName(WorkLog workLog, String taskName);

    List<WorkLogDetail> findByWorkLogAndTaskName(WorkLog workLog, String taskName);

    @Query("SELECT wld FROM WorkLogDetail wld WHERE wld.workLog.uuid = :worklogUuid AND wld.taskName = :taskName")
    List<WorkLogDetail> findByWorkLogUuidAndTaskName(@Param("worklogUuid") String worklogUuid, @Param("taskName") String taskName);

    List<WorkLogDetail> findByWorkLogAndStatus(WorkLog workLog, WorkLogStatus status);

    @Query("SELECT wld FROM WorkLogDetail wld WHERE wld.workLog = :workLog AND (:status IS NULL OR wld.status = :status)")
    List<WorkLogDetail> findAllByWorkLogAndStatus(@Param("workLog") WorkLog workLog, @Param("status") WorkLogStatus status);

    @Query("SELECT wld FROM WorkLogDetail wld WHERE wld.workLog = :workLog AND wld.taskName IN :taskNames AND (:status IS NULL OR wld.status = :status)")
    List<WorkLogDetail> findByWorkLogAndTaskNameInAndStatus(@Param("workLog") WorkLog workLog, @Param("taskNames") List<String> taskNames, @Param("status") WorkLogStatus status);

    @Query("SELECT wld FROM WorkLogDetail wld WHERE wld.workLog = :workLog AND wld.uuid IN :uuids AND (:status IS NULL OR wld.status = :status)")
    List<WorkLogDetail> findByWorkLogAndUuidInAndStatus(@P("workLog") WorkLog workLog, @Param("uuids") List<String> uuids, @Param("status") WorkLogStatus status);

    @Query("SELECT DISTINCT wld.workLog.uuid FROM WorkLogDetail wld WHERE wld.workLog.id IN :workLogIds AND wld.syncError IS NOT NULL")
    List<String> findWorkLogUuidsWithSyncErrors(@Param("workLogIds") List<Long> workLogIds);

    @Query("SELECT CASE WHEN SUM(CASE WHEN wld.status = 'SYNC_IN_PROGRESS' THEN 1 ELSE 0 END) > 0 THEN 'SYNC_IN_PROGRESS' " +
            "WHEN SUM(CASE WHEN wld.status = 'UNSYNC_IN_PROGRESS' THEN 1 ELSE 0 END) > 0 THEN 'UNSYNC_IN_PROGRESS' " +
            "WHEN SUM(CASE WHEN wld.status = 'IN_QUEUE' THEN 1 ELSE 0 END) > 0 THEN 'IN_QUEUE' " +
            "WHEN SUM(CASE WHEN wld.status = 'NOT_SYNCED' THEN 1 ELSE 0 END) = 0 THEN 'SYNCED' " +
            "WHEN SUM(CASE WHEN wld.status = 'SYNCED' THEN 1 ELSE 0 END) = 0 THEN 'NOT_SYNCED' " +
            "ELSE 'PARTIALLY' END " +
            "FROM WorkLogDetail wld WHERE wld.workLog.uuid = :workLogUuid AND wld.taskName = :taskName " +
            "GROUP BY wld.taskName")
    WorkLogStatus calculateWorkLogTaskStatus(@Param("workLogUuid") String workLogUuid, @Param("taskName") String taskName);

    @Modifying
    @Query("DELETE FROM WorkLogDetail wld WHERE wld.workLog.id IN :workLogsIds")
    void deleteByWorkLogIn(List<Long> workLogsIds);
}
