package com.unavaliability.backend.controllers;

import com.unavaliability.backend.dto.EventoDtos.EventoView;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.EventoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;
    private final CurrentUserProvider currentUser;

    public EventoController(EventoService eventoService, CurrentUserProvider currentUser) {
        this.eventoService = eventoService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Map<String, List<EventoView>> list() {
        currentUser.require();
        return Map.of("eventos", eventoService.list());
    }
}
