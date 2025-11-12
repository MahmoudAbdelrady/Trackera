package com.mdevs.trackera.dto.jira;

import java.util.Map;

public record JiraTaskResponse(String key, Map<String, Object> fields) {
    public String getSummary() {
        return (String) fields.get("summary");
    }

    public Map<String, Object> getProject() {
        return (Map<String, Object>) fields.get("project");
    }

    public Map<String, Object> getStatus() {
        return (Map<String, Object>) fields.get("status");
    }

    public Map<String, Object> getTimeTracking() {
        return (Map<String, Object>) fields.get("timetracking");
    }

    public Object getResolution() {
        return fields.get("resolution");
    }
}