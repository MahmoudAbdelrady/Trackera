package com.mdevs.trackera.dto.worklog;

import com.mdevs.trackera.entity.WorkLogDetail;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class DetailUpdateResult {
    private List<WorkLogDetail> updatedDetails;
    private List<WorkLogDetail> detailsToUnsync;
    private WorkLogDetail singleDetail;
    private String oldTaskName;
    private boolean taskChanged;
}
