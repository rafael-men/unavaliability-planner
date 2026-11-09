package com.unavaliability.backend.service;

import com.unavaliability.backend.domain.Status;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.PasswordTicket;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.PasswordTicketRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.AuthorizationService;
import com.unavaliability.backend.util.TextUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;


@Service
public class PasswordTicketService {

    private static final int RESET_TOKEN = 32;
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private final SecureRandom secureRandom = new SecureRandom();
    private final String frontendUrl;
    private final PasswordTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthorizationService authz;

    public PasswordTicketService(
        PasswordTicketRepository ticketRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        EmailService emailService,
        AuthorizationService authz,
        @Value("${app.frontend-url:http://localhost:3000}")
        String frontendUrl) {
    this.ticketRepository = ticketRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
    this.authz = authz;
    this.frontendUrl = frontendUrl;
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
        if (ticketRepository.existsByEmailIgnoreCaseAndStatus(email, Status.Ticket.OPEN)) {
            return;
        }
        ticketRepository.save(new PasswordTicket(email, user.getId()));
    }

    @Transactional(readOnly = true)
    public List<PasswordTicket> list(User actor, boolean onlyOpen) {
        authz.requireAdminEditor(actor);
        return onlyOpen
                ? ticketRepository.findByStatusOrderByCreatedAtDesc(Status.Ticket.OPEN)
                : ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return ticketRepository.findByStatusOrderByCreatedAtDesc(Status.Ticket.OPEN).size();
    }


    @Transactional
    public void resolve(User actor, Long ticketId) {
        authz.requireAdminEditor(actor);

        PasswordTicket ticket = ticketRepository.findById(ticketId).orElse(null);

        if (ticket == null) {
            throw ApiException.notFound("Ticket não encontrado.");
        }

        if (Status.Ticket.RESOLVED.equals(ticket.getStatus())) {
            throw ApiException.badRequest("Ticket já foi resolvido.");
        }

        User user = ticket.getUserId() != null
                ? userRepository.findById(ticket.getUserId()).orElse(null)
                : userRepository.findByEmailIgnoreCase(ticket.getEmail()).orElse(null);

        if (user == null) {
            throw ApiException.notFound("Usuário do ticket não encontrado.");
        }

        byte[] tokenBytes = new byte[RESET_TOKEN];
        secureRandom.nextBytes(tokenBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        ticket.setTokenHash(hashToken(rawToken));
        ticket.setTokenExpiresAt(OffsetDateTime.now().plus(RESET_TOKEN_TTL));
        ticket.setTokenUsedAt(null);
        ticket.setResolvedBy(actor.getId());
        ticketRepository.save(ticket);

        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;

        emailService.send(
                user.getEmail(),
                "Redefinição de senha",
                "Olá " + (user.getNome() != null ? user.getNome() : "") + ",\n\n"
                        + "Foi autorizada uma redefinição de senha para sua conta.\n\n"
                        + "Acesse o link abaixo para escolher uma nova senha:\n"
                        + resetLink + "\n\n"
                        + "Este link expira em 30 minutos e pode ser usado uma única vez.\n\n"
                        + "Se você não solicitou esta alteração, ignore este e-mail.\n");
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        validatePassword(newPassword);

        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.badRequest("Token inválido ou expirado.");
        }

        String tokenHash = hashToken(rawToken);
        PasswordTicket ticket = ticketRepository
                .findByTokenHashAndStatus(tokenHash, Status.Ticket.OPEN)
                .orElseThrow(() -> ApiException.badRequest("Token inválido ou expirado."));

        if (ticket.getTokenUsedAt() != null
                || ticket.getTokenExpiresAt() == null
                || ticket.getTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw ApiException.badRequest("Token inválido ou expirado.");
        }

        User user = ticket.getUserId() != null
                ? userRepository.findById(ticket.getUserId()).orElse(null)
                : userRepository.findByEmailIgnoreCase(ticket.getEmail()).orElse(null);

        if (user == null) {
            throw ApiException.badRequest("Token inválido ou expirado.");
        }

        user.setPassw(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ticket.setStatus(Status.Ticket.RESOLVED);
        ticket.setTokenUsedAt(OffsetDateTime.now());
        ticket.setResolvedAt(OffsetDateTime.now());
        ticketRepository.save(ticket);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw ApiException.badRequest("Senha deve ter pelo menos 6 caracteres.");
        }

        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw ApiException.badRequest(
                    "Senha deve conter ao menos uma letra e um número.");
        }
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não disponível.", exception);
        }
    }
}
