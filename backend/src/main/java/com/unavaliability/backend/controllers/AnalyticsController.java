package com.unavaliability.backend.controllers;

import com.unavaliability.backend.dto.AnalyticsDtos.DashboardAnalitico;
import com.unavaliability.backend.dto.AnalyticsDtos.ForecastResponse;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Métricas, dashboard analítico e forecast de capacidade")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserProvider currentUser;

    public AnalyticsController(AnalyticsService analyticsService, CurrentUserProvider currentUser) {
        this.analyticsService = analyticsService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Dashboard analítico",
            description = "Dias de indisponibilidade por setor/mês, taxa de aprovação, "
                    + "tempo médio de aprovação e ranking de conflitos no período. "
                    + "Sem datas, usa do início do ano até hoje.")
    @GetMapping("/dashboard")
    public DashboardAnalitico dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return analyticsService.dashboard(currentUser.require(), de, ate);
    }

    @Operation(summary = "Forecast de capacidade",
            description = "Projeção de quantas pessoas estarão indisponíveis por semana, "
                    + "nas próximas N semanas (1–26, padrão 8).")
    @GetMapping("/forecast")
    public ForecastResponse forecast(
            @RequestParam(name = "semanas", defaultValue = "8") int semanas) {
        return analyticsService.forecast(currentUser.require(), semanas, LocalDate.now());
    }
}
