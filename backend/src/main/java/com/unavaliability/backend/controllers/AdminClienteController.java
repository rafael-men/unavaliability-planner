package com.unavaliability.backend.controllers;

import jakarta.validation.Valid;
import com.unavaliability.backend.dto.ClienteDtos.AssignUserRequest;
import com.unavaliability.backend.dto.ClienteDtos.ClienteRequest;
import com.unavaliability.backend.models.Cliente;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.ClienteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/admin/clientes")
public class AdminClienteController {

    private final ClienteService clienteService;
    private final CurrentUserProvider currentUser;

    public AdminClienteController(ClienteService clienteService, CurrentUserProvider currentUser) {
        this.clienteService = clienteService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ClienteService.ClientesResponse list() {
        return clienteService.listWithLinks(currentUser.require());
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody ClienteRequest req) {
        Cliente c = clienteService.create(currentUser.require(), req);
        return Map.of("success", true, "cliente", c);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody ClienteRequest req) {
        Cliente c = clienteService.update(currentUser.require(), id, req);
        return Map.of("success", true, "cliente", c);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        clienteService.delete(currentUser.require(), id);
        return Map.of("success", true);
    }

    @PostMapping("/{id}/assign")
    public Map<String, Object> assign(@PathVariable Long id, @Valid @RequestBody AssignUserRequest req) {
        clienteService.assign(currentUser.require(), id, req);
        return Map.of("success", true);
    }

    @DeleteMapping("/{id}/assign")
    public Map<String, Object> unassign(@PathVariable Long id, @Valid @RequestBody AssignUserRequest req) {
        clienteService.unassign(currentUser.require(), id, req == null ? null : req.user_id());
        return Map.of("success", true);
    }
}
