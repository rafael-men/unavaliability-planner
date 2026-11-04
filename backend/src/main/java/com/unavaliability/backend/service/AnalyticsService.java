package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.AnalyticsDtos.DashboardAnalitico;
import com.unavaliability.backend.dto.AnalyticsDtos.DiasPorMes;
import com.unavaliability.backend.dto.AnalyticsDtos.DiasPorSetor;
import com.unavaliability.backend.dto.AnalyticsDtos.ForecastResponse;
import com.unavaliability.backend.dto.AnalyticsDtos.RankingConflito;
import com.unavaliability.backend.dto.AnalyticsDtos.SemanaForecast;
import com.unavaliability.backend.dto.AnalyticsDtos.TaxaAprovacao;
import com.unavaliability.backend.dto.AnalyticsDtos.TempoMedioAprovacao;
import com.unavaliability.backend.domain.Status;
import com.unavaliability.backend.models.Unavailability;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.ClienteRepository;
import com.unavaliability.backend.repositories.EventoClienteRepository;
import com.unavaliability.backend.repositories.EventoRepository;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserClienteRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.AuthorizationService;
import com.unavaliability.backend.util.BusinessDays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public class AnalyticsService {

    private final UnavailabilityRepository unavailabilityRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authz;

    public AnalyticsService(UnavailabilityRepository unavailabilityRepository, UserRepository userRepository,
                            UserClienteRepository userClienteRepository, EventoRepository eventoRepository,
                            EventoClienteRepository eventoClienteRepository, ClienteRepository clienteRepository,
                            AuthorizationService authz) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.userRepository = userRepository;
        this.authz = authz;
    }


    @Transactional(readOnly = true)
    public DashboardAnalitico dashboard(User actor, LocalDate de, LocalDate ate) {
      authz.requireCanViewAll(actor);
       LocalDate inicio = de != null
            ? de
            : LocalDate.now().withDayOfYear(1);

    LocalDate fim = ate != null
            ? ate
            : LocalDate.now();

    OffsetDateTime periodoInicio = inicio
            .atStartOfDay()
            .atOffset(ZoneOffset.UTC);

    OffsetDateTime periodoFimExclusivo = fim
            .plusDays(1)
            .atStartOfDay()
            .atOffset(ZoneOffset.UTC);

    return new DashboardAnalitico(
            diasPorSetorSql(inicio, fim),
            diasPorMesSql(inicio, fim),
            taxaAprovacaoSql(periodoInicio, periodoFimExclusivo),
            tempoMedioAprovacaoSql(periodoInicio, periodoFimExclusivo),
            rankingConflitosSql(inicio, fim));
    }

    private List<DiasPorSetor> diasPorSetorSql(
        LocalDate de,
        LocalDate ate) {

    return unavailabilityRepository
            .findAnalyticsDaysByDepartment(de, ate)
            .stream()
            .map(row -> new DiasPorSetor(
                    row.getDepartment(),
                    row.getTotalDays(),
                    row.getSolicitacoes()))
            .toList();
    }
    private List<DiasPorMes> diasPorMesSql(
        LocalDate de,
        LocalDate ate) {

    return unavailabilityRepository
            .findAnalyticsDaysByMonth(de, ate)
            .stream()
            .map(row -> new DiasPorMes(
                    row.getMes(),
                    row.getTotalDays(),
                    row.getSolicitacoes()))
            .toList();
    }
    private TaxaAprovacao taxaAprovacaoSql(
        OffsetDateTime de,
        OffsetDateTime ate) {

    var row = unavailabilityRepository
            .findAnalyticsApprovalRate(de, ate);

    long total = row.getTotal();
    long aprovadas = row.getAprovadas();
    long rejeitadas = row.getRejeitadas();
    long pendentes = row.getPendentes();

    long revisadas = aprovadas + rejeitadas;

    double taxa = revisadas == 0
            ? 0.0
            : Math.round((aprovadas * 10000.0) / revisadas) / 100.0;

    return new TaxaAprovacao(
            total,
            aprovadas,
            rejeitadas,
            pendentes,
            taxa);
    }

    private TempoMedioAprovacao tempoMedioAprovacaoSql(
        OffsetDateTime de,
        OffsetDateTime ate) {

    var row = unavailabilityRepository
            .findAnalyticsAverageApprovalTime(de, ate);

    BigDecimal media = row.getHorasMedia();

    return new TempoMedioAprovacao(
            media != null ? media.doubleValue() : 0.0,
            row.getAmostras());
    }
    private List<RankingConflito> rankingConflitosSql(
        LocalDate de,
        LocalDate ate) {

    return unavailabilityRepository
            .findAnalyticsConflictRanking(de, ate)
            .stream()
            .map(row -> new RankingConflito(
                    row.getClienteId(),
                    row.getClienteNome(),
                    row.getConflitos()))
            .toList();
    }

    //List<DiasPorSetor> diasPorSetor(List<Unavailability> registros) {
        //Map<String, long[]> acc = new LinkedHashMap<>(); // [totalDays, count]
        //for (Unavailability u : registros) {
            //String dep = u.getDepartment() == null ? "(sem setor)" : u.getDepartment();
           // long[] v = acc.computeIfAbsent(dep, k -> new long[2]);
           // v[0] += u.getTotalDays() != null ? u.getTotalDays() : 0;
           // v[1] += 1;
        //}
       // List<DiasPorSetor> out = new ArrayList<>();
      //  acc.forEach((dep, v) -> out.add(new DiasPorSetor(dep, v[0], v[1])));
      //  out.sort(Comparator.comparingLong(DiasPorSetor::totalDays).reversed());
      //  return out;
    //}

    //List<DiasPorMes> diasPorMes(List<Unavailability> registros) {
       // Map<String, long[]> acc = new java.util.TreeMap<>();
     //   for (Unavailability u : registros) {
        //    if (u.getStartDate() == null) {
           //     continue;
          //  }
           // String mes = String.format("%04d-%02d",
              //      u.getStartDate().getYear(), u.getStartDate().getMonthValue());
          //  long[] v = acc.computeIfAbsent(mes, k -> new long[2]);
         //   v[0] += u.getTotalDays() != null ? u.getTotalDays() : 0;
        //    v[1] += 1;
      //  }
      //  List<DiasPorMes> out = new ArrayList<>();
      //  acc.forEach((mes, v) -> out.add(new DiasPorMes(mes, v[0], v[1])));
     //   return out;
    //}

    @Transactional(readOnly = true)
    public ForecastResponse forecast(User actor, int semanas, LocalDate hoje) {
        authz.requireCanViewAll(actor);
        int n = Math.min(Math.max(semanas, 1), 26);
        LocalDate inicio = hoje;
        LocalDate fim = hoje.plusWeeks(n);

        List<Unavailability> aprovadas = unavailabilityRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        Status.Unavailability.APPROVED, fim, inicio);
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
