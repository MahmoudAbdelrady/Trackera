package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkLogDetailRepository extends BaseRepository<WorkLogDetail> {
    void deleteAllByWorkLog(WorkLog workLog);

    @Query("SELECT wld FROM WorkLogDetail wld WHERE wld.workLog = :workLog GROUP BY wld.taskName")
    List<WorkLogDetail> getGroupedWorkLogDetailsByWorkLog(WorkLog workLog);
}
