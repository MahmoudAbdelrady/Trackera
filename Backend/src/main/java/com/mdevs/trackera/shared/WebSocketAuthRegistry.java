package com.mdevs.trackera.shared;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.service.WorkLogService;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

@Component
public class WebSocketAuthRegistry {
    private final WorkLogService workLogService;

    @Getter
    private final Map<String, BiPredicate<User, String>> rules = new HashMap<>();

    public WebSocketAuthRegistry(WorkLogService workLogService) {
        this.workLogService = workLogService;
        rules.put("/topic/log-status/*", this::validateLogStatus);
    }

    //<editor-fold desc="Validation Methods">
    private boolean validateLogStatus(User user, String dest) {
        String[] parts = dest.split("/");
        String logId = parts[parts.length - 1];
        workLogService.ensureWorkLogExistsAndHasPermission(logId, user);
        return true;
    }
    //</editor-fold>
}
