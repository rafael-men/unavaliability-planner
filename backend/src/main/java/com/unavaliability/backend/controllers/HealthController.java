package com.unavaliability.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (!Integer.valueOf(1).equals(result)) {
                return databaseUnavailable("Resposta inesperada do banco");
            }
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "database", "ok"));
        } catch (Exception e) {
            return databaseUnavailable("banco não disponível");
        }
    }

    private ResponseEntity<Map<String, Object>> databaseUnavailable(String message) {
    return ResponseEntity.status(503).body(Map.of(
            "status", "error",
            "database", "unavailable",
            "message", message));
}
}
