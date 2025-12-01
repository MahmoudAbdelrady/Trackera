package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface WorkLogDetailRepository extends BaseRepository<WorkLogDetail> {
    void deleteAllByWorkLog(WorkLog workLog);

    @Query("SELECT NEW MAP(wld.taskName as taskName, MIN(wld.taskUrl) AS taskUrl, SUM(wld.duration) AS totalMinutes, " +
            "CASE WHEN SUM(CASE WHEN wld.status = 'SYNC_IN_PROGRESS' THEN 1 ELSE 0 END) > 0 THEN 'SYNC_IN_PROGRESS' " +
            "     WHEN SUM(CASE WHEN wld.status = 'UNSYNC_IN_PROGRESS' THEN 1 ELSE 0 END) > 0 THEN 'UNSYNC_IN_PROGRESS' " +
            "     WHEN SUM(CASE WHEN wld.status = 'NOT_SYNCED' THEN 1 ELSE 0 END) = 0 THEN 'SYNCED' " +
            "     WHEN SUM(CASE WHEN wld.status = 'SYNCED' THEN 1 ELSE 0 END) = 0 THEN 'NOT_SYNCED' " +
            "     ELSE 'PARTIALLY' END AS status) " +
            "FROM WorkLogDetail wld WHERE wld.workLog = :workLog GROUP BY wld.taskName")
    List<Map<String, Object>> getGroupedWorkLogDetailsByWorkLog(WorkLog workLog);

    List<WorkLogDetail> findAllByWorkLog(WorkLog workLog);

    boolean existsByWorkLog(WorkLog workLog);

    List<WorkLogDetail> findByWorkLogAndTaskName(WorkLog workLog, String taskName);

    List<WorkLogDetail> findByWorkLogAndTaskNameInAndStatusNot(WorkLog workLog, List<String> taskNames, WorkLogStatus status);

    boolean existsByWorkLogAndTaskName(WorkLog workLog, String taskName);

    WorkLogDetail findByUuid(String uuid);

    List<WorkLogDetail> findByWorkLogAndUuidInAndStatusNot(WorkLog workLog, List<String> uuids, WorkLogStatus status);

    List<WorkLogDetail> findByWorkLogAndStatus(WorkLog workLog, WorkLogStatus status);
}
