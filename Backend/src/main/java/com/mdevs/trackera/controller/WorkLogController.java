package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.worklog.ManageWorkLogDTO;
import com.mdevs.trackera.service.WorkLogService;
import com.mdevs.trackera.shared.search_filter.SearchFilter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public WorkLogController(WorkLogService workLogService) {
        this.workLogService = workLogService;
    }

    @PostMapping("/search")
    public ResponseEntity<?> SearchAllWorkLogs(@RequestBody(required = false) List<SearchFilter> searchFilters, Pageable pageable) {
        return new ResponseEntity<>(workLogService.searchAllWorkLogs(searchFilters, pageable), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> AddWorkLog(@RequestPart(name = "worklogInfo") @Valid ManageWorkLogDTO manageWorkLogDTO, @RequestPart MultipartFile file) {
        Map<String, Object> result = workLogService.addWorkLog(manageWorkLogDTO, file);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> UpdateWorkLog(@PathVariable Long id, @RequestPart(name = "worklogInfo") @Valid ManageWorkLogDTO manageWorkLogDTO, @RequestPart(required = false) MultipartFile file) {
        Map<String, Object> result = workLogService.updateWorkLog(id, manageWorkLogDTO, file);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> DeleteWorkLog(@PathVariable Long id) {
        return new ResponseEntity<>(workLogService.deleteWorkLog(id), HttpStatus.OK);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> GetCurrentMonthSummary() {
        return new ResponseEntity<>(workLogService.getCurrentMonthSummary(), HttpStatus.OK);
    }
}
