package com.mdevs.trackera.shared.mappers;

import com.mdevs.trackera.dto.worklog.WorkLogInfoDTO;
import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.entity.WorkLogDetail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkLogMapper {
    WorkLogInfoDTO toDto(WorkLog workLog);
}
