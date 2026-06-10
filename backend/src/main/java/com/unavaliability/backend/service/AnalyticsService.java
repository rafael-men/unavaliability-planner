package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.AnalyticsDtos.DashboardAnalitico;
import com.unavaliability.backend.dto.AnalyticsDtos.DiasPorMes;
import com.unavaliability.backend.dto.AnalyticsDtos.DiasPorSetor;
import com.unavaliability.backend.dto.AnalyticsDtos.ForecastResponse;
import com.unavaliability.backend.dto.AnalyticsDtos.RankingConflito;
import com.unavaliability.backend.dto.AnalyticsDtos.SemanaForecast;
import com.unavaliability.backend.dto.AnalyticsDtos.TaxaAprovacao;
import com.unavaliability.backend.dto.AnalyticsDtos.TempoMedioAprovacao;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Cliente;
import com.unavaliability.backend.models.Evento;
import com.unavaliability.backend.models.EventoCliente;
import com.unavaliability.backend.models.Unavailability;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.models.UserCliente;
import com.unavaliability.backend.repositories.ClienteRepository;
import com.unavaliability.backend.repositories.EventoClienteRepository;
import com.unavaliability.backend.repositories.EventoRepository;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserClienteRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.Roles;
import com.unavaliability.backend.util.BusinessDays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public class AnalyticsService {

    private final UnavailabilityRepository unavailabilityRepository;
    private final UserRepository userRepository;
    private final UserClienteRepository userClienteRepository;
    private final EventoRepository eventoRepository;
    private final EventoClienteRepository eventoClienteRepository;
    private final ClienteRepository clienteRepository;

    public AnalyticsService(UnavailabilityRepository unavailabilityRepository, UserRepository userRepository,
                            UserClienteRepository userClienteRepository, EventoRepository eventoRepository,
                            EventoClienteRepository eventoClienteRepository, ClienteRepository clienteRepository) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.userRepository = userRepository;
        this.userClienteRepository = userClienteRepository;
        this.eventoRepository = eventoRepository;
        this.eventoClienteRepository = eventoClienteRepository;
        this.clienteRepository = clienteRepository;
    }

    private void requireViewAll(User actor) {
        if (!Roles.canViewAll(actor.getRole())) {
            throw ApiException.forbidden("Acesso restrito a administradores.");
        }
    }


    @Transactional(readOnly = true)
    public DashboardAnalitico dashboard(User actor, LocalDate de, LocalDate ate) {
        requireViewAll(actor);
        if (de == null) {
            de = LocalDate.now().withDayOfYear(1);
        }
        if (ate == null) {
            ate = LocalDate.now();
        }

        List<Unavailability> aprovadas = unavailabilityRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual("approved", ate, de);

        List<Unavailability> noPeriodo = unavailabilityRepository.findByCreatedAtBetween(
                de.atStartOfDay().atOffset(ZoneOffset.UTC),
                ate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return new DashboardAnalitico(
                diasPorSetor(aprovadas),
                diasPorMes(aprovadas),
                taxaAprovacao(noPeriodo),
                tempoMedioAprovacao(noPeriodo),
                rankingConflitos(de, ate));
    }

    List<DiasPorSetor> diasPorSetor(List<Unavailability> registros) {
        Map<String, long[]> acc = new LinkedHashMap<>(); // [totalDays, count]
        for (Unavailability u : registros) {
            String dep = u.getDepartment() == null ? "(sem setor)" : u.getDepartment();
            long[] v = acc.computeIfAbsent(dep, k -> new long[2]);
            v[0] += u.getTotalDays() != null ? u.getTotalDays() : 0;
            v[1] += 1;
        }
        List<DiasPorSetor> out = new ArrayList<>();
        acc.forEach((dep, v) -> out.add(new DiasPorSetor(dep, v[0], v[1])));
        out.sort(Comparator.comparingLong(DiasPorSetor::totalDays).reversed());
        return out;
    }

    List<DiasPorMes> diasPorMes(List<Unavailability> registros) {
        Map<String, long[]> acc = new java.util.TreeMap<>();
        for (Unavailability u : registros) {
            if (u.getStartDate() == null) {
                continue;
            }
            String mes = String.format("%04d-%02d",
                    u.getStartDate().getYear(), u.getStartDate().getMonthValue());
            long[] v = acc.computeIfAbsent(mes, k -> new long[2]);
            v[0] += u.getTotalDays() != null ? u.getTotalDays() : 0;
            v[1] += 1;
        }
        List<DiasPorMes> out = new ArrayList<>();
        acc.forEach((mes, v) -> out.add(new DiasPorMes(mes, v[0], v[1])));
        return out;
    }

    TaxaAprovacao taxaAprovacao(List<Unavailability> registros) {
        long total = registros.size();
        long aprovadas = registros.stream().filter(u -> "approved".equals(u.getStatus())).count();
        long rejeitadas = registros.stream().filter(u -> "rejected".equals(u.getStatus())).count();
        long pendentes = registros.stream().filter(u -> "pending".equals(u.getStatus())).count();
        long revisadas = aprovadas + rejeitadas;
        double taxa = revisadas == 0 ? 0.0 : Math.round((aprovadas * 10000.0) / revisadas) / 100.0;
        return new TaxaAprovacao(total, aprovadas, rejeitadas, pendentes, taxa);
    }

    TempoMedioAprovacao tempoMedioAprovacao(List<Unavailability> registros) {
        long soma = 0;
        long amostras = 0;
        for (Unavailability u : registros) {
            if (u.getReviewedAt() != null && u.getCreatedAt() != null) {
                long horas = ChronoUnit.HOURS.between(u.getCreatedAt(), u.getReviewedAt());
                if (horas >= 0) {
                    soma += horas;
                    amostras++;
                }
            }
        }
        double media = amostras == 0 ? 0.0 : Math.round((soma * 100.0) / amostras) / 100.0;
        return new TempoMedioAprovacao(media, amostras);
    }

    List<RankingConflito> rankingConflitos(LocalDate de, LocalDate ate) {
        List<Unavailability> aprovadas = unavailabilityRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual("approved", ate, de);
        if (aprovadas.isEmpty()) {
            return List.of();
        }
        List<Evento> eventos = eventoRepository
                .findByDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioAsc(ate, de);
        if (eventos.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = new HashSet<>();
        aprovadas.forEach(u -> userIds.add(u.getUserId()));
        Map<Long, Set<Long>> clientesByUser = new HashMap<>();
        for (UserCliente uc : userClienteRepository.findByIdUserIdInAndAtivoTrue(new ArrayList<>(userIds))) {
            clientesByUser.computeIfAbsent(uc.getId().getUserId(), k -> new HashSet<>())
                    .add(uc.getId().getClienteId());
        }
        if (clientesByUser.isEmpty()) {
            return List.of();
        }

        List<Long> eventoIds = eventos.stream().map(Evento::getId).toList();
        Map<Long, Set<Long>> clientesByEvento = new HashMap<>();
        for (EventoCliente ec : eventoClienteRepository.findByIdEventoIdIn(eventoIds)) {
            clientesByEvento.computeIfAbsent(ec.getId().getEventoId(), k -> new HashSet<>())
                    .add(ec.getId().getClienteId());
        }

        Map<Long, Long> conflitosPorCliente = new HashMap<>();
        for (Unavailability u : aprovadas) {
            Set<Long> userClientes = clientesByUser.get(u.getUserId());
            if (userClientes == null || userClientes.isEmpty()) {
                continue;
            }
            for (Evento ev : eventos) {
                boolean overlap = !ev.getDataInicio().isAfter(u.getEndDate())
                        && !ev.getDataFim().isBefore(u.getStartDate());
                if (!overlap) {
                    continue;
                }
                Set<Long> evClientes = clientesByEvento.getOrDefault(ev.getId(), Set.of());
                for (Long cid : evClientes) {
                    if (userClientes.contains(cid)) {
                        conflitosPorCliente.merge(cid, 1L, Long::sum);
                    }
                }
            }
        }
        if (conflitosPorCliente.isEmpty()) {
            return List.of();
        }

        Map<Long, String> nomePorCliente = new HashMap<>();
        for (Cliente c : clienteRepository.findAllById(conflitosPorCliente.keySet())) {
            nomePorCliente.put(c.getId(), c.getNome());
        }

        List<RankingConflito> out = new ArrayList<>();
        conflitosPorCliente.forEach((cid, n) ->
                out.add(new RankingConflito(cid, nomePorCliente.getOrDefault(cid, "?"), n)));
        out.sort(Comparator.comparingLong(RankingConflito::conflitos).reversed());
        return out;
    }

    @Transactional(readOnly = true)
    public ForecastResponse forecast(User actor, int semanas, LocalDate hoje) {
        requireViewAll(actor);
        int n = Math.min(Math.max(semanas, 1), 26);
        LocalDate inicio = hoje;
        LocalDate fim = hoje.plusWeeks(n);

        List<Unavailability> aprovadas = unavailabilityRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual("approved", fim, inicio);
        Map<Long, User> userById = new HashMap<>();
        Set<Long> ids = new HashSet<>();
        aprovadas.forEach(u -> ids.add(u.getUserId()));
        if (!ids.isEmpty()) {
            userRepository.findAllById(ids).forEach(u -> userById.put(u.getId(), u));
        }

        List<SemanaForecast> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDate ws = hoje.plusWeeks(i);
            LocalDate we = ws.plusDays(6);
            List<String> nomes = new ArrayList<>();
            for (Unavailability u : aprovadas) {
                boolean overlap = !u.getStartDate().isAfter(we) && !u.getEndDate().isBefore(ws);
                if (overlap) {
                    User ru = userById.get(u.getUserId());
                    nomes.add(ru != null && ru.getNome() != null ? ru.getNome() : u.getFullName());
                }
            }
            out.add(new SemanaForecast(ws.toString(), we.toString(), nomes.size(), nomes));
        }
        return new ForecastResponse(out);
    }

    static int diasUteis(LocalDate ini, LocalDate fim) {
        return BusinessDays.count(ini, fim);
    }
}
