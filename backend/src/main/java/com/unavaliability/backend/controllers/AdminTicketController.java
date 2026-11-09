package com.unavaliability.backend.controllers;

import com.unavaliability.backend.models.PasswordTicket;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.PasswordTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/admin/tickets")
@Tag(name = "Tickets", description = "Tickets de redefinição de senha (admin)")
public class AdminTicketController {

    private final PasswordTicketService ticketService;
    private final CurrentUserProvider currentUser;

    public AdminTicketController(PasswordTicketService ticketService, CurrentUserProvider currentUser) {
        this.ticketService = ticketService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Lista tickets de senha")
    @GetMapping
    public List<PasswordTicket> list(@RequestParam(name = "onlyOpen", defaultValue = "false") boolean onlyOpen) {
        return ticketService.list(currentUser.require(), onlyOpen);
    }

    @Operation(summary = "Autoriza redefinição e envia link seguro por e-mail")
    @PostMapping("/{id}/resolve")
    public Map<String, Object> resolve(@PathVariable Long id) {
        ticketService.resolve(currentUser.require(), id);
        return Map.of("success", true, "message", "Link de redefinição enviado por e-mail.");
    }
}
