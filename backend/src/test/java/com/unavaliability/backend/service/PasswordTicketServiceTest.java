package com.unavaliability.backend.service;

import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.PasswordTicket;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.PasswordTicketRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordTicketService — abertura neutra e resolução")
class PasswordTicketServiceTest {

    @Mock PasswordTicketRepository ticketRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;

    PasswordTicketService service;

    private User admin;


    private final AuthorizationService authz = new AuthorizationService();

    @BeforeEach
    void setUp() {
        service = new PasswordTicketService(ticketRepository, userRepository, passwordEncoder, emailService, authz, "http://localhost:3000");
        admin = new User();
        admin.setId(1L);
        admin.setRole("admin_editor");
    }

    @Test
    @DisplayName("open com e-mail inexistente NÃO cria ticket (anti-enumeração)")
    void openEmailInexistente() {
        when(userRepository.findByEmailIgnoreCase("naoexiste@x.com")).thenReturn(Optional.empty());
        service.open("naoexiste@x.com");
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("open com e-mail inválido não faz nada")
    void openEmailInvalido() {
        service.open("sem-arroba");
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("open não duplica quando já há ticket aberto")
    void openNaoDuplica() {
        User u = new User(); u.setId(9L); u.setEmail("a@x.com");
        when(userRepository.findByEmailIgnoreCase("a@x.com")).thenReturn(Optional.of(u));
        when(ticketRepository.existsByEmailIgnoreCaseAndStatus("a@x.com", "open")).thenReturn(true);
        service.open("a@x.com");
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("open com e-mail válido e existente cria ticket aberto")
    void openCriaTicket() {
        User u = new User(); u.setId(9L); u.setEmail("a@x.com");
        when(userRepository.findByEmailIgnoreCase("a@x.com")).thenReturn(Optional.of(u));
        when(ticketRepository.existsByEmailIgnoreCaseAndStatus("a@x.com", "open")).thenReturn(false);

        service.open("A@x.com"); 

        ArgumentCaptor<PasswordTicket> captor = ArgumentCaptor.forClass(PasswordTicket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("a@x.com");
        assertThat(captor.getValue().getUserId()).isEqualTo(9L);
        assertThat(captor.getValue().getStatus()).isEqualTo("open");
    }


    @Test
    @DisplayName("resolve exige admin editor (403 para colaborador)")
    void resolveSemPermissao() {
        User colaborador = new User(); colaborador.setRole("colaborador");
        assertThatThrownBy(() -> service.resolve(colaborador, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Admin Editor");
    }

    @Test
    @DisplayName("reset rejeita senha fraca (sem número)")
    void resetSenhaFraca() {
        assertThatThrownBy(() -> service.reset("token", "apenasletras"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("letra e um número");
    }

    @Test
    @DisplayName("resolve em ticket já resolvido falha")
    void resolveJaResolvido() {
        PasswordTicket t = new PasswordTicket("a@x.com", 9L);
        t.setStatus("resolved");
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.resolve(admin, 5L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("já foi resolvido");
    }

    @Test
    @DisplayName("resolve gera token e envia link sem senha")
    void resolveOk() {
        PasswordTicket t = new PasswordTicket("a@x.com", 9L);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(t));
        User dono = new User(); dono.setId(9L); dono.setEmail("a@x.com"); dono.setNome("Ana");
        when(userRepository.findById(9L)).thenReturn(Optional.of(dono));
        service.resolve(admin, 5L);

        assertThat(t.getTokenHash()).isNotBlank();
        assertThat(t.getTokenExpiresAt()).isAfter(OffsetDateTime.now());
        assertThat(t.getStatus()).isEqualTo("open");
        assertThat(t.getResolvedBy()).isEqualTo(1L);
        verify(userRepository, never()).save(dono);
        verify(passwordEncoder, never()).encode(anyString());
        verify(emailService).send(
                eq("a@x.com"),
                eq("Redefinição de senha"),
                org.mockito.ArgumentMatchers.argThat(message ->
                        message.contains("reset-password?token=")
                                && !message.contains("senha123")));
    }

    @Test
    @DisplayName("reset troca a senha e invalida o token")
    void resetOk() {
        PasswordTicket t = new PasswordTicket("a@x.com", 9L);
        String token = "token-de-teste";
        t.setTokenHash(hashForTest(token));
        t.setTokenExpiresAt(OffsetDateTime.now().plusMinutes(10));

        User dono = new User();
        dono.setId(9L);
        dono.setEmail("a@x.com");

        when(ticketRepository.findByTokenHashAndStatus(t.getTokenHash(), "open"))
                .thenReturn(Optional.of(t));
        when(userRepository.findById(9L)).thenReturn(Optional.of(dono));
        when(passwordEncoder.encode("Nova123")).thenReturn("$2a$10$novo");

        service.reset(token, "Nova123");

        assertThat(dono.getPassw()).isEqualTo("$2a$10$novo");
        assertThat(t.getStatus()).isEqualTo("resolved");
        assertThat(t.getTokenUsedAt()).isNotNull();
        verify(userRepository).save(dono);
        verify(ticketRepository).save(t);
    }

    private static String hashForTest(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
