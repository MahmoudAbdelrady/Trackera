package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.WorkLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface WorkLogRepository extends BaseRepository<WorkLog> {
    boolean existsByWorkDate(LocalDate workDate);

    @Query("SELECT w FROM WorkLog w ORDER BY w.workDate DESC")
    Page<WorkLog> findAllOrderByWorkDateDesc(Pageable pageable);
}
