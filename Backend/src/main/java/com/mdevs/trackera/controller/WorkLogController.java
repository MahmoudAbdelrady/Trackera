package com.mdevs.trackera.controller;

import com.mdevs.trackera.dto.worklog.NewWorkLogDTO;
import com.mdevs.trackera.service.WorkLogService;
import com.mdevs.trackera.shared.exceptions.ExceptionResponseMaker;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/worklog")
public class WorkLogController {
    private final WorkLogService workLogService;

    @Autowired
    public WorkLogController(WorkLogService workLogService) {
        this.workLogService = workLogService;
    }

    @GetMapping("/all")
    public ResponseEntity<?> GetAllWorkLogs(Pageable pageable) {
        return new ResponseEntity<>(workLogService.getAllWorklogs(pageable), HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<?> AddWorkLog(@RequestPart(name = "worklog") @Valid NewWorkLogDTO newWorkLogDTO, @RequestPart MultipartFile file) {
        Map<String, Object> result = workLogService.addWorkLog(newWorkLogDTO, file);
        return result.containsKey("isError") ? new ResponseEntity<>(result, HttpStatus.BAD_REQUEST) : new ResponseEntity<>(result.get("message"), HttpStatus.CREATED);
    }
}
