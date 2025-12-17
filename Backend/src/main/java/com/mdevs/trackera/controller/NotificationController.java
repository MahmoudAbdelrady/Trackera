package com.mdevs.trackera.controller;

import com.mdevs.trackera.service.NotificationService;
import com.mdevs.trackera.utils.CookieHelper;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe(@CookieValue(value = CookieHelper.ACCESS_TOKEN_COOKIE_NAME, required = false) String accessToken) {
        return notificationService.createSubscription(accessToken);
    }
}
