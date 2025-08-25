package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.WorkLog;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface WorkLogRepository extends BaseRepository<WorkLog> {
    boolean existsByWorkDate(LocalDate workDate);
}
