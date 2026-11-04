package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.AnalyticsDtos.DashboardAnalitico;
import com.unavaliability.backend.repositories.ClienteRepository;
import com.unavaliability.backend.repositories.EventoClienteRepository;
import com.unavaliability.backend.repositories.EventoRepository;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserClienteRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.AuthorizationService;
import com.unavaliability.backend.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    UnavailabilityRepository unavailabilityRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    UserClienteRepository userClienteRepository;

    @Mock
    EventoRepository eventoRepository;

    @Mock
    EventoClienteRepository eventoClienteRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    AuthorizationService authorizationService;

    @Mock
    UnavailabilityRepository.AnalyticsSectorProjection sector;

    @Mock
    UnavailabilityRepository.AnalyticsMonthProjection month;

    @Mock
    UnavailabilityRepository.AnalyticsApprovalProjection approval;

    @Mock
    UnavailabilityRepository.AnalyticsAverageApprovalProjection average;

    @Mock
    UnavailabilityRepository.AnalyticsConflictProjection conflict;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(
                unavailabilityRepository,
                userRepository,
                userClienteRepository,
                eventoRepository,
                eventoClienteRepository,
                clienteRepository,
                authorizationService);
    }

    @Test
    void dashboardUsesAggregatedRepositoryQueries() {
        User actor = new User();
        actor.setRole("admin_leitor");

        LocalDate de = LocalDate.of(2026, 1, 1);
        LocalDate ate = LocalDate.of(2026, 3, 31);
        OffsetDateTime inicio = de.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fimExclusivo = ate.plusDays(1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        when(sector.getDepartment()).thenReturn("Tecnologia");
        when(sector.getTotalDays()).thenReturn(12L);
        when(sector.getSolicitacoes()).thenReturn(3L);

        when(month.getMes()).thenReturn("2026-03");
        when(month.getTotalDays()).thenReturn(8L);
        when(month.getSolicitacoes()).thenReturn(2L);

        when(approval.getTotal()).thenReturn(10L);
        when(approval.getAprovadas()).thenReturn(6L);
        when(approval.getRejeitadas()).thenReturn(2L);
        when(approval.getPendentes()).thenReturn(2L);

        when(average.getHorasMedia()).thenReturn(new BigDecimal("14.50"));
        when(average.getAmostras()).thenReturn(8L);

        when(conflict.getClienteId()).thenReturn(5L);
        when(conflict.getClienteNome()).thenReturn("Cliente A");
        when(conflict.getConflitos()).thenReturn(4L);

        when(unavailabilityRepository.findAnalyticsDaysByDepartment(de, ate))
                .thenReturn(List.of(sector));
        when(unavailabilityRepository.findAnalyticsDaysByMonth(de, ate))
                .thenReturn(List.of(month));
        when(unavailabilityRepository.findAnalyticsApprovalRate(inicio, fimExclusivo))
                .thenReturn(approval);
        when(unavailabilityRepository.findAnalyticsAverageApprovalTime(inicio, fimExclusivo))
                .thenReturn(average);
        when(unavailabilityRepository.findAnalyticsConflictRanking(de, ate))
                .thenReturn(List.of(conflict));

        DashboardAnalitico result = service.dashboard(actor, de, ate);

        assertThat(result.dias_por_setor())
                .extracting("department", "totalDays", "solicitacoes")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        "Tecnologia", 12L, 3L));
        assertThat(result.dias_por_mes())
                .extracting("mes", "totalDays", "solicitacoes")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        "2026-03", 8L, 2L));
        assertThat(result.taxa_aprovacao())
                .extracting(
                        "total",
                        "aprovadas",
                        "rejeitadas",
                        "pendentes",
                        "taxaAprovacaoPct")
                .containsExactly(10L, 6L, 2L, 2L, 75.0);
        assertThat(result.tempo_medio_aprovacao().horasMedia()).isEqualTo(14.5);
        assertThat(result.tempo_medio_aprovacao().amostras()).isEqualTo(8L);
        assertThat(result.ranking_conflitos())
                .extracting("clienteId", "clienteNome", "conflitos")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        5L, "Cliente A", 4L));

        verify(authorizationService).requireCanViewAll(actor);
        verify(unavailabilityRepository).findAnalyticsDaysByDepartment(de, ate);
        verify(unavailabilityRepository).findAnalyticsDaysByMonth(de, ate);
        verify(unavailabilityRepository).findAnalyticsApprovalRate(inicio, fimExclusivo);
        verify(unavailabilityRepository)
                .findAnalyticsAverageApprovalTime(inicio, fimExclusivo);
        verify(unavailabilityRepository).findAnalyticsConflictRanking(de, ate);

        verify(unavailabilityRepository, never())
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        eq("approved"), eq(ate), eq(de));
        verify(unavailabilityRepository, never())
                .findByCreatedAtBetween(inicio, fimExclusivo);
    }

    @Test
    void dashboardUsesCurrentYearAndTodayWhenDatesAreNull() {
        User actor = new User();
        actor.setRole("admin_leitor");

        when(unavailabilityRepository.findAnalyticsDaysByDepartment(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(unavailabilityRepository.findAnalyticsDaysByMonth(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(unavailabilityRepository.findAnalyticsApprovalRate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(approval);
        when(unavailabilityRepository.findAnalyticsAverageApprovalTime(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(average);
        when(unavailabilityRepository.findAnalyticsConflictRanking(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        when(approval.getTotal()).thenReturn(0L);
        when(approval.getAprovadas()).thenReturn(0L);
        when(approval.getRejeitadas()).thenReturn(0L);
        when(approval.getPendentes()).thenReturn(0L);
        when(average.getHorasMedia()).thenReturn(BigDecimal.ZERO);
        when(average.getAmostras()).thenReturn(0L);

        DashboardAnalitico result = service.dashboard(actor, null, null);

        assertThat(result.taxa_aprovacao().taxaAprovacaoPct()).isZero();
        assertThat(result.tempo_medio_aprovacao().horasMedia()).isZero();
        verify(authorizationService).requireCanViewAll(actor);
    }

}
