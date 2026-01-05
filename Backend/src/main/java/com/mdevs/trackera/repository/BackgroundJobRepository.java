package com.mdevs.trackera.repository;

import com.mdevs.trackera.entity.BackgroundJob;
import com.mdevs.trackera.shared.enums.BackgroundJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackgroundJobRepository extends BaseRepository<BackgroundJob> {
    List<BackgroundJob> findByStatusInAndIdAfterOrderById(List<BackgroundJobStatus> statusList, Long maxId, Pageable pageable);
}
