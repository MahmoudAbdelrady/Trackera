package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.worklog.WorkLogSelectionDTO;
import com.mdevs.trackera.dto.worklog.ManageWorkLogDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSearchFilterDTO;
import com.mdevs.trackera.service.WorkLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/worklog")
public class WorkLogController {
    private final WorkLogService workLogService;

    @PostMapping("/search")
    public ResponseEntity<?> searchAllWorkLogs(@RequestBody(required = false) WorkLogSearchFilterDTO searchFilterDTO, Pageable pageable) {
        return new ResponseEntity<>(workLogService.searchAllWorkLogs(searchFilterDTO, pageable), HttpStatus.OK);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<?> getWorkLogByUUID(@PathVariable String uuid) {
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
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.OK);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<?> deleteWorkLog(@PathVariable String uuid, @RequestBody(required = false) WorkLogSelectionDTO workLogSelectionDTO) {
        return new ResponseEntity<>(workLogService.deleteWorkLog(uuid, workLogSelectionDTO), HttpStatus.OK);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getCurrentMonthSummary() {
        return new ResponseEntity<>(workLogService.getCurrentMonthSummary(), HttpStatus.OK);
    }

    @GetMapping("/{uuid}/details")
    public ResponseEntity<?> getWorkLogTasks(@PathVariable String uuid) {
        return new ResponseEntity<>(workLogService.getWorkLogTasks(uuid), HttpStatus.OK);
    }

    @GetMapping("/{uuid}/details/task")
    public ResponseEntity<?> getWorkLogTaskEntries(@PathVariable String uuid, @RequestParam String taskName) {
        return new ResponseEntity<>(workLogService.getWorkLogTaskEntries(uuid, taskName), HttpStatus.OK);
    }

    @PostMapping("/{uuid}/sync")
    public ResponseEntity<?> syncWorkLog(@PathVariable String uuid, @RequestBody(required = false) WorkLogSelectionDTO workLogSelectionDTO, @RequestParam(required = false, defaultValue = "true") boolean sync) {
        workLogService.performJiraSync(uuid, workLogSelectionDTO, sync);
        return new ResponseEntity<>((sync ? "Sync" : "Unsync") + " request initiated successfully", HttpStatus.OK);
    }
}
