package com.unavaliability.backend.controllers;

import jakarta.validation.Valid;
import com.unavaliability.backend.dto.EventoDtos.EventoRequest;
import com.unavaliability.backend.dto.EventoDtos.EventoView;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.EventoService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/admin/eventos")
public class AdminEventoController {

    private final EventoService eventoService;
    private final CurrentUserProvider currentUser;

    public AdminEventoController(EventoService eventoService, CurrentUserProvider currentUser) {
        this.eventoService = eventoService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Map<String, List<EventoView>> list() {
        currentUser.require();
        return Map.of("eventos", eventoService.list());
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody EventoRequest req) {
        EventoView e = eventoService.create(currentUser.require(), req);
        return Map.of("success", true, "evento", e);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody EventoRequest req) {
        EventoView e = eventoService.update(currentUser.require(), id, req);
        return Map.of("success", true, "evento", e);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        eventoService.delete(currentUser.require(), id);
        return Map.of("success", true);
    }
}
