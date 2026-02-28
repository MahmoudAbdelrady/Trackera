package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.worklog.*;
import com.mdevs.trackera.service.WorkLogService;
import com.mdevs.trackera.shared.enums.WorklogSyncOperation;
import com.mdevs.trackera.shared.annotations.RateLimited;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/worklog")
public class WorkLogController {
    private final WorkLogService workLogService;

    @PostMapping("/search")
    public ResponseEntity<Page<WorkLogInfoDTO>> searchAllWorkLogs(@RequestBody(required = false) WorkLogSearchFilterDTO searchFilterDTO, Pageable pageable) {
        return new ResponseEntity<>(workLogService.searchAllWorkLogs(searchFilterDTO, pageable), HttpStatus.OK);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<WorkLogInfoDTO> getWorkLogByUUID(@PathVariable String uuid) {
        return new ResponseEntity<>(workLogService.getWorkLogByUUID(uuid), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> addWorkLog(@RequestPart(name = "worklogInfo") @Valid ManageWorkLogDTO manageWorkLogDTO, @RequestPart MultipartFile file) {
        Map<String, Object> result = workLogService.addWorkLog(manageWorkLogDTO, file);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.CREATED);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<?> updateWorkLog(@PathVariable String uuid, @RequestPart(name = "worklogInfo") @Valid ManageWorkLogDTO manageWorkLogDTO, @RequestPart(required = false) MultipartFile file) {
        Map<String, Object> result = workLogService.updateWorkLog(uuid, manageWorkLogDTO, file);
        return new ResponseEntity<>(result, result.containsKey("isError") ? HttpStatus.BAD_REQUEST : HttpStatus.OK);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Map<String, Object>> deleteWorkLog(@PathVariable String uuid, @RequestBody(required = false) WorkLogSelectionDTO workLogSelectionDTO) {
        return new ResponseEntity<>(workLogService.deleteWorkLog(uuid, workLogSelectionDTO), HttpStatus.OK);
    }

    @GetMapping("/summary")
    public ResponseEntity<WorklogSummaryResultDTO> getCurrentMonthSummary() {
        return new ResponseEntity<>(workLogService.getCurrentMonthSummary(), HttpStatus.OK);
    }

    @GetMapping("/{uuid}/details")
    public ResponseEntity<List<WorkLogTaskDTO>> getWorkLogTasks(@PathVariable String uuid) {
        return new ResponseEntity<>(workLogService.getWorkLogTasks(uuid), HttpStatus.OK);
    }

    @GetMapping("/{uuid}/details/task")
    public ResponseEntity<List<WorkLogEntryDTO>> getWorkLogTaskEntries(@PathVariable String uuid, @RequestParam String taskName) {
        return new ResponseEntity<>(workLogService.getWorkLogTaskEntries(uuid, taskName), HttpStatus.OK);
    }

    @PutMapping("/{uuid}/details")
    public ResponseEntity<UpdateWorkLogDetailResponseDTO> updateWorkLogDetail(@PathVariable String uuid, @RequestBody @Valid UpdateWorkLogDetailPayloadDTO payload) {
        return new ResponseEntity<>(workLogService.updateWorkLogDetail(uuid, payload), HttpStatus.OK);
    }

    @RateLimited(permitsPerMinute = 60)
    @PostMapping("/{uuid}/sync")
    public ResponseEntity<String> syncWorkLog(@PathVariable String uuid, @RequestBody(required = false) WorkLogSelectionDTO workLogSelectionDTO, @RequestParam(required = false, defaultValue = "SYNC") WorklogSyncOperation operation) {
        workLogService.performJiraSync(uuid, workLogSelectionDTO, operation);
        return new ResponseEntity<>(operation.getLabel() + " request initiated successfully", HttpStatus.OK);
    }
}
