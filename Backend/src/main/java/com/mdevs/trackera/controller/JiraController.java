package com.mdevs.trackera.controller;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.service.JiraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/jira")
public class JiraController {
    private final JiraService jiraService;

    @GetMapping("/tasks")
    public ResponseEntity<?> getUserTasks(@RequestParam(required = false, defaultValue = "false") boolean forceUpdate) {
        return ResponseEntity.ok(jiraService.getUserTasks(forceUpdate));
    }

    @GetMapping("/sites")
    public ResponseEntity<?> getUserSites() {
        return ResponseEntity.ok(jiraService.getUserSites(AppConfig.getAuthenticatedCurrentUser()));
    }
}
