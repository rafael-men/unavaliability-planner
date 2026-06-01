package com.unavaliability.backend.controllers;

import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUser;

    public NotificationController(NotificationService notificationService, CurrentUserProvider currentUser) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public NotificationService.NotificationsResponse list() {
        return notificationService.list(currentUser.require());
    }
}
