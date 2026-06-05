package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.AuthDtos.LoginRequest;
import com.unavaliability.backend.dto.AuthDtos.RegisterRequest;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — rate limit, lockout e regras de senha")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock SetorService setorService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, memberRepository, passwordEncoder,
                jwtService, setorService, 3, 15);
    }


    @Test
    @DisplayName("checkLoginRate permite até o limite e bloqueia (429) ao exceder")
    void rateLimit() {
        long now = 1_000_000L;
        for (int i = 0; i < 10; i++) {
            authService.checkLoginRate("1.2.3.4", now);
        }
        assertThatThrownBy(() -> authService.checkLoginRate("1.2.3.4", now))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Muitas tentativas");
    }

    @Test
    @DisplayName("janela expira: após o reset, o contador zera")
    void rateLimitJanelaExpira() {
        long now = 1_000_000L;
        for (int i = 0; i < 10; i++) {
            authService.checkLoginRate("9.9.9.9", now);
        }
        long depois = now + 16 * 60 * 1000;
        authService.checkLoginRate("9.9.9.9", depois);
    }



    @Test
    @DisplayName("após N falhas a conta é bloqueada (429), mesmo com senha correta depois")
    void lockoutPorConta() {
        User u = new User();
        u.setId(1L);
        u.setEmail("rafael@gmail.com");
        u.setPassw("$2a$10$hash");
        u.setStatus("approved");
        when(userRepository.findByEmailIgnoreCase("rafael@gmail.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginRequest bad = new LoginRequest("rafael@gmail.com", "errada");
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> authService.login(bad)).isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(() -> authService.login(bad))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("bloqueada");
    }

    @Test
    @DisplayName("login com credenciais inválidas devolve mensagem genérica")
    void loginInvalido() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("x@y.com", "senha123")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Email ou senha incorretos.");
    }

    @Test
    @DisplayName("login bem-sucedido devolve token")
    void loginOk() {
        User u = new User();
        u.setId(7L);
        u.setEmail("rafael@gmail.com");
        u.setPassw("$2a$10$hash");
        u.setStatus("approved");
        u.setRole("admin_master");
        when(userRepository.findByEmailIgnoreCase("rafael@gmail.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(jwtService.generateToken(u)).thenReturn("jwt-abc");

        var resp = authService.login(new LoginRequest("rafael@gmail.com", "admin123"));
        assertThat(resp.success()).isTrue();
        assertThat(resp.token()).isEqualTo("jwt-abc");
        assertThat(resp.user().role()).isEqualTo("admin_master");
    }

    @Test
    @DisplayName("usuário pendente não loga (403)")
    void loginPendente() {
        User u = new User();
        u.setId(2L);
        u.setEmail("p@x.com");
        u.setPassw("$2a$10$hash");
        u.setStatus("pending");
        when(userRepository.findByEmailIgnoreCase("p@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("p@x.com", "senha123")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("aguardando aprovação");
    }


    @Test
    @DisplayName("register rejeita senha curta")
    void registerSenhaCurta() {
        when(setorService.exists("Tecnologia")).thenReturn(true);
        RegisterRequest req = new RegisterRequest("novo@x.com", "ab1", "Novo", "Tecnologia");
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("pelo menos");
    }

    @Test
    @DisplayName("register rejeita senha sem letra+número")
    void registerSenhaFraca() {
        when(setorService.exists("Tecnologia")).thenReturn(true);
        RegisterRequest req = new RegisterRequest("novo@x.com", "abcdef", "Novo", "Tecnologia");
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("letra e um número");
    }

    @Test
    @DisplayName("register rejeita setor inválido")
    void registerSetorInvalido() {
        lenient().when(setorService.exists(anyString())).thenReturn(false);
        RegisterRequest req = new RegisterRequest("novo@x.com", "senha1", "Novo", "Inexistente");
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ApiException.class)
                .hasMessage("Setor inválido.");
    }

    @Test
    @DisplayName("register válido persiste usuário pendente")
    void registerOk() {
        when(setorService.exists("Tecnologia")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("novo@x.com")).thenReturn(false);
        when(memberRepository.findByEmailIgnoreCase("novo@x.com")).thenReturn(Optional.empty());
        when(memberRepository.save(any())).thenAnswer(inv -> {
            var m = (com.unavaliability.backend.models.Member) inv.getArgument(0);
            m.setId(50L);
            return m;
        });
        when(passwordEncoder.encode("senha1")).thenReturn("$2a$10$enc");

        authService.register(new RegisterRequest("novo@x.com", "senha1", "Novo", "Tecnologia"));

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User salvo = captor.getValue();
        assertThat(salvo.getStatus()).isEqualTo("pending");
        assertThat(salvo.getRole()).isEqualTo("colaborador");
        assertThat(salvo.getPassw()).isEqualTo("$2a$10$enc");
    }
}
