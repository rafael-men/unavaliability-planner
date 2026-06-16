package com.unavaliability.backend.service;

import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.PasswordTicket;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.PasswordTicketRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.Roles;
import com.unavaliability.backend.util.TextUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;


@Service
public class PasswordTicketService {

    private final PasswordTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordTicketService(PasswordTicketRepository ticketRepository, UserRepository userRepository,
                                 PasswordEncoder passwordEncoder, EmailService emailService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    private void requireAdminEditor(User actor) {
        if (!Roles.isAdminEditor(actor.getRole())) {
            throw ApiException.forbidden("Acesso restrito a administradores.");
        }
    }


    @Transactional
    public void open(String emailRaw) {
        if (emailRaw == null || !TextUtils.isValidEmail(emailRaw.toLowerCase().trim())) {
            return;
        }
        String email = emailRaw.toLowerCase().trim();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return;
        }
        if (ticketRepository.existsByEmailIgnoreCaseAndStatus(email, "open")) {
            return;
        }
        ticketRepository.save(new PasswordTicket(email, user.getId()));
    }

    @Transactional(readOnly = true)
    public List<PasswordTicket> list(User actor, boolean onlyOpen) {
        requireAdminEditor(actor);
        return onlyOpen
                ? ticketRepository.findByStatusOrderByCreatedAtDesc("open")
                : ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return ticketRepository.findByStatusOrderByCreatedAtDesc("open").size();
    }


    @Transactional
    public void resolve(User actor, Long ticketId, String newPassword) {
        requireAdminEditor(actor);
        PasswordTicket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            throw ApiException.notFound("Ticket não encontrado.");
        }
        if ("resolved".equals(ticket.getStatus())) {
            throw ApiException.badRequest("Ticket já foi resolvido.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw ApiException.badRequest("Senha deve ter pelo menos 6 caracteres.");
        }
        boolean hasLetter = newPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw ApiException.badRequest("Senha deve conter ao menos uma letra e um número.");
        }

        User user = ticket.getUserId() != null
                ? userRepository.findById(ticket.getUserId()).orElse(null)
                : userRepository.findByEmailIgnoreCase(ticket.getEmail()).orElse(null);
        if (user == null) {
            throw ApiException.notFound("Usuário do ticket não encontrado.");
        }

        user.setPassw(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        ticket.setStatus("resolved");
        ticket.setResolvedBy(actor.getId());
        ticket.setResolvedAt(OffsetDateTime.now());
        ticketRepository.save(ticket);

        emailService.send(user.getEmail(), "Sua senha foi redefinida",
                "Olá " + (user.getNome() != null ? user.getNome() : "") + ",\n\n"
                        + "Sua senha de acesso ao Sistema de Indisponibilidade foi redefinida por um "
                        + "administrador.\n\nNova senha: " + newPassword + "\n\n"
                        + "Recomendamos alterá-la após o primeiro acesso.\n");
    }
}
