package com.unavaliability.backend.service;

import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Unavailability;
import com.unavaliability.backend.models.UnavailabilityAudit;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.ClienteRepository;
import com.unavaliability.backend.repositories.EventoClienteRepository;
import com.unavaliability.backend.repositories.EventoRepository;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UnavailabilityAuditRepository;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserClienteRepository;
import com.unavaliability.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnavailabilityService.cancelOrShorten — cancelar / antecipar + auditoria + e-mail")
class UnavailabilityCancelTest {

    @Mock UnavailabilityRepository unavailabilityRepository;
    @Mock UserRepository userRepository;
    @Mock MemberRepository memberRepository;
    @Mock UserClienteRepository userClienteRepository;
    @Mock EventoRepository eventoRepository;
    @Mock EventoClienteRepository eventoClienteRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock SetorService setorService;
    @Mock UnavailabilityAuditRepository auditRepository;
    @Mock EmailService emailService;

    UnavailabilityService service;

    private static final LocalDate HOJE = LocalDate.of(2024, 1, 1);
    private User admin;

    @BeforeEach
    void setUp() {
        service = new UnavailabilityService(unavailabilityRepository, userRepository, memberRepository,
                userClienteRepository, eventoRepository, eventoClienteRepository, clienteRepository,
                setorService, auditRepository, emailService);
        admin = new User();
        admin.setId(1L);
        admin.setNome("Admin");
        admin.setRole("admin_editor");
    }

    private Unavailability aprovada() {
        Unavailability u = new Unavailability();
        u.setId(10L);
        u.setUserId(7L);
        u.setFullName("Bruno");
        u.setStatus("approved");
        u.setStartDate(LocalDate.of(2024, 1, 22)); // seg
        u.setEndDate(LocalDate.of(2024, 1, 26));    // sex (5 úteis)
        u.setTotalDays(5);
        return u;
    }

    @Test
    @DisplayName("colaborador não pode cancelar (403)")
    void semPermissao() {
        User colaborador = new User(); colaborador.setId(7L); colaborador.setRole("colaborador");
        assertThatThrownBy(() -> service.cancelOrShorten(colaborador, 10L, null, HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("administradores ou líderes");
        verify(unavailabilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("não cancela pedido que não está aprovado")
    void naoAprovado() {
        Unavailability u = aprovada();
        u.setStatus("pending");
        when(unavailabilityRepository.findById(10L)).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.cancelOrShorten(admin, 10L, null, HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("aprovadas");
    }

    @Test
    @DisplayName("cancelamento total: status 'canceled', auditoria e e-mail ao dono")
    void cancelaTudo() {
        Unavailability u = aprovada();
        when(unavailabilityRepository.findById(10L)).thenReturn(Optional.of(u));
        User dono = new User(); dono.setId(7L); dono.setEmail("bruno@x.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(dono));

        service.cancelOrShorten(admin, 10L, null, HOJE);

        assertThat(u.getStatus()).isEqualTo("canceled");
        verify(unavailabilityRepository).save(u);
        verify(emailService).send(eqArg("bruno@x.com"), anyString(), anyString());

        ArgumentCaptor<UnavailabilityAudit> audit = ArgumentCaptor.forClass(UnavailabilityAudit.class);
        verify(auditRepository).save(audit.capture());
        assertThat(audit.getValue().getAction()).isEqualTo("canceled");
    }

    @Test
    @DisplayName("retorno antecipado: encurta fim e recalcula dias úteis")
    void antecipaRetorno() {
        Unavailability u = aprovada();
        when(unavailabilityRepository.findById(10L)).thenReturn(Optional.of(u));
        User dono = new User(); dono.setId(7L); dono.setEmail("bruno@x.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(dono));
        service.cancelOrShorten(admin, 10L, LocalDate.of(2024, 1, 24), HOJE);

        assertThat(u.getStatus()).isEqualTo("approved"); // continua aprovada
        assertThat(u.getEndDate()).isEqualTo(LocalDate.of(2024, 1, 24));
        assertThat(u.getTotalDays()).isEqualTo(3);
        verify(emailService).send(eqArg("bruno@x.com"), anyString(), anyString());

        ArgumentCaptor<UnavailabilityAudit> audit = ArgumentCaptor.forClass(UnavailabilityAudit.class);
        verify(auditRepository).save(audit.capture());
        assertThat(audit.getValue().getAction()).isEqualTo("shortened");
    }

    @Test
    @DisplayName("nova data de retorno >= fim atual é inválida")
    void novaDataInvalida() {
        Unavailability u = aprovada();
        when(unavailabilityRepository.findById(10L)).thenReturn(Optional.of(u));
        lenient().when(userRepository.findById(7L)).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> service.cancelOrShorten(admin, 10L, LocalDate.of(2024, 1, 26), HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("anterior ao fim atual");
    }

    private static String eqArg(String v) {
        return org.mockito.ArgumentMatchers.eq(v);
    }
}
