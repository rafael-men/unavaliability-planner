package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.UnavailabilityDtos.CreateRequest;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.Unavailability;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnavailabilityService.create — validações de criação")
class UnavailabilityServiceTest {

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


    private static final LocalDate HOJE = LocalDate.of(2024, 1, 1); // segunda-feira

    private User user;

    @BeforeEach
    void setUp() {
        service = new UnavailabilityService(unavailabilityRepository, userRepository, memberRepository,
                userClienteRepository, eventoRepository, eventoClienteRepository, clienteRepository, setorService,
                auditRepository, emailService);
        user = new User();
        user.setId(1L);
        user.setNome("Rafael");
        user.setRole("colaborador");
    }

    private CreateRequest req(String tipo, LocalDate ini, LocalDate fim, int totalDias) {
        return new CreateRequest(tipo, "Tecnologia", ini, fim, totalDias);
    }

    @Test
    @DisplayName("campos obrigatórios ausentes → 400")
    void camposObrigatorios() {
        CreateRequest r = new CreateRequest(null, null, null, null, null);
        assertThatThrownBy(() -> service.create(user, r, HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Todos os campos");
    }

    @Test
    @DisplayName("tipo inválido → 400")
    void tipoInvalido() {
        CreateRequest r = req("invalido", HOJE.plusDays(20), HOJE.plusDays(24), 5);
        assertThatThrownBy(() -> service.create(user, r, HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Tipo de indisponibilidade inválido");
    }

    @Test
    @DisplayName("setor inválido → 400")
    void setorInvalido() {
        when(setorService.exists("Tecnologia")).thenReturn(false);
        CreateRequest r = req("pontual", HOJE.plusDays(20), HOJE.plusDays(20), 1);
        assertThatThrownBy(() -> service.create(user, r, HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Setor inválido");
    }

    @Test
    @DisplayName("início com menos de 15 dias de antecedência → 400")
    void antecedenciaMinima() {
        when(setorService.exists("Tecnologia")).thenReturn(true);
        CreateRequest r = req("pontual", HOJE.plusDays(10), HOJE.plusDays(10), 1);
        assertThatThrownBy(() -> service.create(user, r, HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("pelo menos 15 dias");
    }

    @Test
    @DisplayName("total de dias úteis divergente do calculado → 400")
    void totalDiasDivergente() {
        when(setorService.exists("Tecnologia")).thenReturn(true);
        LocalDate ini = LocalDate.of(2024, 1, 22);
        CreateRequest r = req("pontual", ini, ini, 5);
        assertThatThrownBy(() -> service.create(user, r, HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Total de dias úteis inválido");
    }

    @Test
    @DisplayName("prolongado com menos de 5 dias úteis → 400")
    void prolongadoMinimo() {
        when(setorService.exists("Tecnologia")).thenReturn(true);
        LocalDate ini = LocalDate.of(2024, 1, 22);
        LocalDate fim = LocalDate.of(2024, 1, 25);
        CreateRequest r = req("prolongado", ini, fim, 4);
        assertThatThrownBy(() -> service.create(user, r, HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no mínimo 5 dias");
    }

    @Test
    @DisplayName("período sobreposto a solicitação ativa → 400")
    void sobreposicao() {
        when(setorService.exists("Tecnologia")).thenReturn(true);
        LocalDate ini = LocalDate.of(2024, 1, 22);
        LocalDate fim = LocalDate.of(2024, 1, 26);

        Unavailability existente = new Unavailability();
        existente.setId(99L);
        existente.setStartDate(LocalDate.of(2024, 1, 24));
        existente.setEndDate(LocalDate.of(2024, 1, 30));
        existente.setStatus("approved");
        when(unavailabilityRepository.findByUserIdAndStatusIn(anyLong(), anyList()))
                .thenReturn(List.of(existente));

        CreateRequest r = req("prolongado", ini, fim, 5);
        assertThatThrownBy(() -> service.create(user, r, HOJE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("sobrepõe");
    }

    @Test
    @DisplayName("pedido válido é persistido com status pending e total recalculado")
    void criaComSucesso() {
        when(setorService.exists("Tecnologia")).thenReturn(true);
        when(unavailabilityRepository.findByUserIdAndStatusIn(anyLong(), anyList())).thenReturn(List.of());

        LocalDate ini = LocalDate.of(2024, 1, 22); // seg
        LocalDate fim = LocalDate.of(2024, 1, 26);  // sex → 5 úteis
        service.create(user, req("prolongado", ini, fim, 5), HOJE);

        ArgumentCaptor<Unavailability> captor = ArgumentCaptor.forClass(Unavailability.class);
        verify(unavailabilityRepository).save(captor.capture());
        Unavailability salvo = captor.getValue();
        assertThat(salvo.getStatus()).isEqualTo("pending");
        assertThat(salvo.getTotalDays()).isEqualTo(5);
        assertThat(salvo.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("requisição inválida não chega a salvar")
    void invalidoNaoSalva() {
        lenient().when(setorService.exists("Tecnologia")).thenReturn(true);
        CreateRequest r = req("pontual", HOJE.plusDays(5), HOJE.plusDays(5), 1);
        assertThatThrownBy(() -> service.create(user, r, HOJE)).isInstanceOf(ApiException.class);
        verify(unavailabilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("filtragem de líder carrega usuários e membros em lote")
    void filtraPendenciasDoLiderSemNMaisUm() {
    User lider = new User();
    lider.setId(1L);
    lider.setRole("lider");
    lider.setEmail("lider@empresa.com");
    lider.setDepartment("Tecnologia");

    Member liderMember = new Member();
    liderMember.setId(10L);
    liderMember.setName("Líder");
    liderMember.setEmail("lider@empresa.com");

    User requesterMesmoDepartamento = new User();
    requesterMesmoDepartamento.setId(2L);
    requesterMesmoDepartamento.setEmail("ana@empresa.com");
    requesterMesmoDepartamento.setDepartment("Tecnologia");
    requesterMesmoDepartamento.setMemberId(20L);

    User requesterReportTo = new User();
    requesterReportTo.setId(3L);
    requesterReportTo.setEmail("bruno@empresa.com");
    requesterReportTo.setDepartment("Operações");
    requesterReportTo.setMemberId(30L);

    Member memberMesmoDepartamento = new Member();
    memberMesmoDepartamento.setId(20L);
    memberMesmoDepartamento.setEmail("ana@empresa.com");
    memberMesmoDepartamento.setReportTo(null);

    Member memberReportTo = new Member();
    memberReportTo.setId(30L);
    memberReportTo.setEmail("bruno@empresa.com");
    memberReportTo.setReportTo("lider@empresa.com");

    Unavailability primeira = new Unavailability();
    primeira.setId(101L);
    primeira.setUserId(2L);
    primeira.setStatus("pending");

    Unavailability segunda = new Unavailability();
    segunda.setId(102L);
    segunda.setUserId(3L);
    segunda.setStatus("pending");

    Unavailability propria = new Unavailability();
    propria.setId(103L);
    propria.setUserId(1L);
    propria.setStatus("pending");

    when(memberRepository.findByEmailIgnoreCase("lider@empresa.com"))
            .thenReturn(Optional.of(liderMember));

    when(unavailabilityRepository.findByStatusOrderByCreatedAtDesc("pending"))
            .thenReturn(List.of(primeira, segunda, propria));

    when(userRepository.findAllById(any()))
            .thenReturn(List.of(
                    requesterMesmoDepartamento,
                    requesterReportTo));

    when(memberRepository.findAllById(any()))
            .thenReturn(List.of(
                    memberMesmoDepartamento,
                    memberReportTo));

    when(memberRepository.findByEmailInIgnoreCase(anyList()))
            .thenReturn(List.of(
                    memberMesmoDepartamento,
                    memberReportTo));

    List<?> result = service.pendingForNotifications(lider);

    assertThat(result).hasSize(2);

    verify(userRepository, times(2)).findAllById(any());
    verify(memberRepository).findAllById(any());
    verify(memberRepository).findByEmailInIgnoreCase(anyList());

    verify(userRepository, never()).findById(2L);
    verify(userRepository, never()).findById(3L);

    verify(memberRepository, never()).findById(20L);
    verify(memberRepository, never()).findById(30L);

    verify(memberRepository, never())
            .findByEmailIgnoreCase("ana@empresa.com");

    verify(memberRepository, never())
            .findByEmailIgnoreCase("bruno@empresa.com");
    }
}
