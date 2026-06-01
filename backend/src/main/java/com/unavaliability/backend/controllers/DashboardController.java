package com.unavaliability.backend.controllers;

import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUser;

    public DashboardController(DashboardService dashboardService, CurrentUserProvider currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Object dashboard() {
        return dashboardService.dashboard(currentUser.require(), LocalDate.now());
    }
}
