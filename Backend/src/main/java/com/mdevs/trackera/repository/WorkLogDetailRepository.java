package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface WorkLogDetailRepository extends BaseRepository<WorkLogDetail> {
    void deleteAllByWorkLog(WorkLog workLog);

    @Query("SELECT NEW MAP(wld.taskName as taskName, MIN(wld.taskUrl) AS taskUrl, SUM(wld.duration) AS totalMinutes, " +
            "CASE WHEN SUM(CASE WHEN wld.synced = false THEN 1 ELSE 0 END) = 0 THEN 'SYNCED' " +
            "     WHEN SUM(CASE WHEN wld.synced = true THEN 1 ELSE 0 END) = 0 THEN 'NOT_SYNCED'" +
            "     ELSE 'PARTIALLY' END AS status) " +
            "FROM WorkLogDetail wld WHERE wld.workLog = :workLog GROUP BY wld.taskName")
    List<Map<String, Object>> getGroupedWorkLogDetailsByWorkLog(WorkLog workLog);

    List<WorkLogDetail> findAllByWorkLog(WorkLog workLog);

    boolean existsByWorkLog(WorkLog workLog);

    List<WorkLogDetail> findByWorkLogAndTaskName(WorkLog workLog, String taskName);

    List<WorkLogDetail> findByWorkLogAndTaskNameIn(WorkLog workLog, List<String> taskNames);

    boolean existsByWorkLogAndTaskName(WorkLog workLog, String taskName);

    WorkLogDetail findByUuid(String uuid);

    List<WorkLogDetail> findByWorkLogAndUuidIn(WorkLog workLog, List<String> uuids);
}
