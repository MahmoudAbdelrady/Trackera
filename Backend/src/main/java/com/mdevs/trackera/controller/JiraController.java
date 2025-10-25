package com.mdevs.trackera.controller;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.service.JiraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/jira")
public class JiraController {
    private final JiraService jiraService;

    public JiraController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> GetUserTasks(@RequestParam(required = false, defaultValue = "false") boolean forceUpdate) {
        return ResponseEntity.ok(jiraService.getUserTasks(forceUpdate));
    }

    @GetMapping("/sites")
    public ResponseEntity<?> GetUserSites() {
        return ResponseEntity.ok(jiraService.getUserSites(Objects.requireNonNull(AppConfig.getCurrentUser())));
    }
}
