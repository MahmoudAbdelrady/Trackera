package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.shared.enums.BackgroundJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackgroundJobRepository extends BaseRepository<BackgroundJob> {
    @Query("SELECT bj.id FROM BackgroundJob bj WHERE bj.status IN :statusList AND bj.id > :maxId ORDER BY bj.id ASC")
    List<Long> findByStatusInAndIdAfterOrderById(@Param("statusList") List<BackgroundJobStatus> statusList, @Param("maxId") Long maxId, Pageable pageable);
}
