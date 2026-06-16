package com.unavaliability.backend.controllers;

import jakarta.validation.Valid;
import com.unavaliability.backend.dto.UnavailabilityDtos.CreateRequest;
import com.unavaliability.backend.dto.UnavailabilityDtos.ImpactResponse;
import com.unavaliability.backend.dto.UnavailabilityDtos.ListResponse;
import com.unavaliability.backend.dto.UnavailabilityDtos.UnavailabilityView;
import com.unavaliability.backend.dto.UnavailabilityDtos.UpdateRequest;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.UnavailabilityService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/unavailability")
public class UnavailabilityController {

    private final UnavailabilityService service;
    private final CurrentUserProvider currentUser;

    public UnavailabilityController(UnavailabilityService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody CreateRequest req) {
        service.create(currentUser.require(), req, LocalDate.now());
        return Map.of("success", true, "message", "Solicitação de indisponibilidade enviada com sucesso.");
    }

    @GetMapping
    public ListResponse list() {
        return service.list(currentUser.require());
    }

    @GetMapping("/mine")
    public List<UnavailabilityView> mine() {
        return service.mine(currentUser.require());
    }

    @GetMapping("/active")
    public List<UnavailabilityView> active() {
        currentUser.require();
        return service.active(LocalDate.now());
    }

    @GetMapping("/pending")
    public List<UnavailabilityView> pending() {
        return service.pending(currentUser.require());
    }

    @GetMapping("/impact")
    public ImpactResponse impact() {
        return service.impact(currentUser.require(), LocalDate.now());
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody UpdateRequest req) {
        service.update(currentUser.require(), id, req, LocalDate.now());
        return Map.of("success", true);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        service.delete(currentUser.require(), id);
        return Map.of("success", true);
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id) {
        service.approve(currentUser.require(), id);
        return Map.of("success", true);
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(@PathVariable Long id) {
        service.reject(currentUser.require(), id);
        return Map.of("success", true);
    }


    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable Long id, @RequestBody(required = false) CancelRequest req) {
        java.time.LocalDate newEnd = req != null ? req.new_end_date() : null;
        service.cancelOrShorten(currentUser.require(), id, newEnd, LocalDate.now());
        return Map.of("success", true);
    }

    public record CancelRequest(java.time.LocalDate new_end_date) {
    }

    @GetMapping("/{id}/history")
    public List<com.unavaliability.backend.models.UnavailabilityAudit> history(@PathVariable Long id) {
        return service.history(currentUser.require(), id);
    }
}
