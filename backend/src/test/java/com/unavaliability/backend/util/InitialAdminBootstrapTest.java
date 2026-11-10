package com.unavaliability.backend.util;

import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InitialAdminBootstrap — criação do admin inicial")
class InitialAdminBootstrapTest {

    private static final String SENHA = "senha-forte-123";

    @Mock UserRepository userRepository;
    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;

    private InitialAdminBootstrap bootstrap(String email, String senha) {
        return new InitialAdminBootstrap(userRepository, memberRepository, passwordEncoder,
                email, senha, "Admin Teste");
    }

    @Test
    @DisplayName("não faz nada quando users5 já tem usuários")
    void naoCriaQuandoJaExisteUsuario() throws Exception {
        when(userRepository.count()).thenReturn(3L);

        bootstrap("admin@empresa.com", SENHA).run(null);

        verify(userRepository, never()).save(any(User.class));
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("cria admin_master aprovado com senha em BCrypt quando a tabela está vazia")
    void criaAdminQuandoTabelaVazia() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(memberRepository.findByEmailIgnoreCase("admin@empresa.com")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(5L);
            return m;
        });
        when(passwordEncoder.encode(SENHA)).thenReturn("$2a$10$hash");

        bootstrap("Admin@Empresa.com", SENHA).run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User salvo = captor.getValue();
        assertThat(salvo.getEmail()).isEqualTo("admin@empresa.com");
        assertThat(salvo.getRole()).isEqualTo("admin_master");
        assertThat(salvo.getStatus()).isEqualTo("approved");
        assertThat(salvo.getPassw()).isEqualTo("$2a$10$hash");
        assertThat(salvo.getMemberId()).isEqualTo(5L);
        assertThat(salvo.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("reaproveita o member existente em vez de criar outro")
    void reaproveitaMemberExistente() throws Exception {
        Member existente = new Member();
        existente.setId(9L);
        existente.setEmail("admin@empresa.com");

        when(userRepository.count()).thenReturn(0L);
        when(memberRepository.findByEmailIgnoreCase("admin@empresa.com")).thenReturn(Optional.of(existente));
        when(passwordEncoder.encode(SENHA)).thenReturn("$2a$10$hash");

        bootstrap("admin@empresa.com", SENHA).run(null);

        verify(memberRepository, never()).save(any(Member.class));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("sem ADMIN_EMAIL ou senha, não cria nada e não quebra o startup")
    void semVariaveisNaoCria() throws Exception {
        when(userRepository.count()).thenReturn(0L);

        bootstrap("", SENHA).run(null);
        bootstrap("admin@empresa.com", "").run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("rejeita senha inicial curta")
    void rejeitaSenhaCurta() {
        when(userRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> bootstrap("admin@empresa.com", "curta").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");

        verify(userRepository, never()).save(any(User.class));
    }
}
