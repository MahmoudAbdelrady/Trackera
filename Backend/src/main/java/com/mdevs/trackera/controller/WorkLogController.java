package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.worklog.ManageWorkLogDTO;
import com.mdevs.trackera.dto.worklog.WorkLogSearchFilterDTO;
import com.mdevs.trackera.service.WorkLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/worklog")
public class WorkLogController {
    private final WorkLogService workLogService;

    public WorkLogController(WorkLogService workLogService) {
        this.workLogService = workLogService;
    }

    @PostMapping("/search")
    public ResponseEntity<?> SearchAllWorkLogs(@RequestBody(required = false)WorkLogSearchFilterDTO searchFilterDTO, Pageable pageable) {
        return new ResponseEntity<>(workLogService.searchAllWorkLogs(searchFilterDTO, pageable), HttpStatus.OK);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<?> GetWorkLogByUUID(@PathVariable String uuid) {
        return new ResponseEntity<>(workLogService.getWorkLogByUUID(uuid), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> AddWorkLog(@RequestPart(name = "worklogInfo") @Valid ManageWorkLogDTO manageWorkLogDTO, @RequestPart MultipartFile file) {
        Map<String, Object> result = workLogService.addWorkLog(manageWorkLogDTO, file);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.CREATED);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<?> UpdateWorkLog(@PathVariable String uuid, @RequestPart(name = "worklogInfo") @Valid ManageWorkLogDTO manageWorkLogDTO, @RequestPart(required = false) MultipartFile file) {
        Map<String, Object> result = workLogService.updateWorkLog(uuid, manageWorkLogDTO, file);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.OK);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<?> DeleteWorkLog(@PathVariable String uuid) {
        workLogService.deleteWorkLog(uuid);
        return new ResponseEntity<>("Worklog deleted successfully", HttpStatus.OK);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> GetCurrentMonthSummary() {
        return new ResponseEntity<>(workLogService.getCurrentMonthSummary(), HttpStatus.OK);
    }

    @GetMapping("/{uuid}/details")
    public ResponseEntity<?> GetWorkLogDetailSummary(@PathVariable String uuid) {
        return new ResponseEntity<>(workLogService.getWorkLogDetailSummary(uuid), HttpStatus.OK);
    }

    @GetMapping("/{uuid}/details/task")
    public ResponseEntity<?> GetWorkLogTaskDetails(@PathVariable String uuid, @RequestParam String taskName) {
        return new ResponseEntity<>(workLogService.getWorkLogTaskDetails(uuid, taskName), HttpStatus.OK);
    }

    @DeleteMapping("/{uuid}/details/task")
    public ResponseEntity<?> DeleteWorkLogTaskDetails(@PathVariable String uuid, @RequestParam String taskName) {
        return new ResponseEntity<>(workLogService.deleteWorkLogTaskDetails(uuid, taskName), HttpStatus.OK);
    }

    @DeleteMapping("/details/entry/{uuid}")
    public ResponseEntity<?> DeleteWorkLogTaskEntry(@PathVariable String uuid) {
        return new ResponseEntity<>(workLogService.deleteWorkLogTaskEntry(uuid), HttpStatus.OK);
    }

    @PostMapping("/{uuid}/sync")
    public ResponseEntity<?> SyncWorkLog(@PathVariable String uuid) {
        Map<String, Object> result = workLogService.syncToJira(uuid);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.OK);
    }

    @PostMapping("/{uuid}/sync/tasks")
    public ResponseEntity<?> SyncWorkLogTasks(@PathVariable String uuid, @RequestParam(name = "tasks") List<String> taskNames) {
        Map<String, Object> result = workLogService.syncTasksToJira(uuid, taskNames);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.OK);
    }

    @PostMapping("/{uuid}/sync/entries")
    public ResponseEntity<?> SyncWorkLogEntries(@PathVariable String uuid, @RequestParam(name = "ids") List<String> entryUuids) {
        Map<String, Object> result = workLogService.syncEntriesToJira(uuid, entryUuids);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.OK);
    }

    @PostMapping("/{uuid}/un-sync")
    public ResponseEntity<?> UnSyncWorkLog(@PathVariable String uuid) {
        Map<String, Object> result = workLogService.unSyncFromJira(uuid);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.OK);
    }

    @PostMapping("/{uuid}/un-sync/tasks")
    public ResponseEntity<?> UnSyncWorkLogTasks(@PathVariable String uuid, @RequestParam(name = "tasks") List<String> taskNames) {
        Map<String, Object> result = workLogService.unSyncTasksFromJira(uuid, taskNames);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.OK);
    }

    @PostMapping("/{uuid}/un-sync/entries")
    public ResponseEntity<?> UnSyncWorkLogEntries(@PathVariable String uuid, @RequestParam(name = "ids") List<String> entryUuids) {
        Map<String, Object> result = workLogService.unSyncEntriesFromJira(uuid, entryUuids);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.OK);
    }
}
