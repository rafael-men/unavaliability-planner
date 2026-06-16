package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.UnavailabilityDtos.CreateRequest;
import com.unavaliability.backend.dto.UnavailabilityDtos.DepartmentImpact;
import com.unavaliability.backend.dto.UnavailabilityDtos.EventConflict;
import com.unavaliability.backend.dto.UnavailabilityDtos.ImpactResponse;
import com.unavaliability.backend.dto.UnavailabilityDtos.ListResponse;
import com.unavaliability.backend.dto.UnavailabilityDtos.UnavailabilityView;
import com.unavaliability.backend.dto.UnavailabilityDtos.UpdateRequest;
import com.unavaliability.backend.dto.UnavailabilityDtos.UsersOnLeave;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.*;
import com.unavaliability.backend.repositories.ClienteRepository;
import com.unavaliability.backend.repositories.EventoClienteRepository;
import com.unavaliability.backend.repositories.EventoRepository;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UnavailabilityAuditRepository;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserClienteRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.Roles;
import com.unavaliability.backend.util.BusinessDays;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public class UnavailabilityService {

    private static final int LIST_LIMIT = 500;
    private static final Set<String> VALID_TYPES = Set.of("prolongado", "pontual");

    private final UnavailabilityRepository unavailabilityRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final UserClienteRepository userClienteRepository;
    private final EventoRepository eventoRepository;
    private final EventoClienteRepository eventoClienteRepository;
    private final ClienteRepository clienteRepository;
    private final SetorService setorService;
    private final UnavailabilityAuditRepository auditRepository;
    private final EmailService emailService;

    public UnavailabilityService(UnavailabilityRepository unavailabilityRepository,
                                 UserRepository userRepository, MemberRepository memberRepository,
                                 UserClienteRepository userClienteRepository,
                                 EventoRepository eventoRepository,
                                 EventoClienteRepository eventoClienteRepository,
                                 ClienteRepository clienteRepository, SetorService setorService,
                                 UnavailabilityAuditRepository auditRepository, EmailService emailService) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.userClienteRepository = userClienteRepository;
        this.eventoRepository = eventoRepository;
        this.eventoClienteRepository = eventoClienteRepository;
        this.clienteRepository = clienteRepository;
        this.setorService = setorService;
        this.auditRepository = auditRepository;
        this.emailService = emailService;
    }


    private void audit(Long unavId, String action, User actor, String detail) {
        auditRepository.save(new UnavailabilityAudit(
                unavId, action,
                actor != null ? actor.getId() : null,
                actor != null ? actor.getNome() : null,
                detail));
    }

  
    private String ownerEmail(Long userId) {
        return userRepository.findById(userId).map(User::getEmail).orElse(null);
    }


    @Transactional
    public void create(User user, CreateRequest req, LocalDate today) {
        if (req == null || isBlank(req.unavailability_type()) || isBlank(req.department())
                || req.start_date() == null || req.end_date() == null || req.total_days() == null) {
            throw ApiException.badRequest("Todos os campos são obrigatórios.");
        }
        if (!VALID_TYPES.contains(req.unavailability_type())) {
            throw ApiException.badRequest("Tipo de indisponibilidade inválido.");
        }
        if (!setorService.exists(req.department())) {
            throw ApiException.badRequest("Setor inválido.");
        }
        LocalDate start = req.start_date();
        LocalDate end = req.end_date();
        if (end.isBefore(start)) {
            throw ApiException.badRequest("Data de retorno deve ser posterior à data de início.");
        }
        if (start.isBefore(today.plusDays(15))) {
            throw ApiException.badRequest("A data de início deve ser pelo menos 15 dias a partir de hoje.");
        }
        int expectedDays = BusinessDays.count(start, end);
        if (expectedDays == 0) {
            throw ApiException.badRequest("O período selecionado não contém dias úteis.");
        }
        if (req.total_days() != expectedDays) {
            throw ApiException.badRequest("Total de dias úteis inválido. Esperado: " + expectedDays + ".");
        }
        if ("prolongado".equals(req.unavailability_type()) && expectedDays < 5) {
            throw ApiException.badRequest("A solicitação de indisponibilidade deve ter no mínimo 5 dias úteis.");
        }
        checkOverlap(user.getId(), start, end, null);

        Unavailability u = new Unavailability();
        u.setUserId(user.getId());
        u.setFullName(user.getNome());
        u.setUnavailabilityType(req.unavailability_type());
        u.setDepartment(req.department());
        u.setStartDate(start);
        u.setEndDate(end);
        u.setTotalDays(expectedDays);
        u.setStatus("pending");
        unavailabilityRepository.save(u);
        audit(u.getId(), "created", user,
                "Solicitação criada (" + start + " a " + end + ", " + expectedDays + " dias úteis).");
    }


    private void checkOverlap(Long userId, LocalDate start, LocalDate end, Long excludeId) {
        List<Unavailability> existing =
                unavailabilityRepository.findByUserIdAndStatusIn(userId, List.of("pending", "approved"));
        for (Unavailability r : existing) {
            if (excludeId != null && excludeId.equals(r.getId())) {
                continue;
            }
            boolean overlap = !start.isAfter(r.getEndDate()) && !r.getStartDate().isAfter(end);
            if (overlap) {
                String statusLabel = "approved".equals(r.getStatus()) ? "aprovada" : "pendente";
                throw ApiException.badRequest("Período se sobrepõe a uma solicitação " + statusLabel
                        + " (" + r.getStartDate() + " a " + r.getEndDate()
                        + "). Cancele ou aguarde a conclusão antes de solicitar um novo período sobreposto.");
            }
        }
    }

    @Transactional(readOnly = true)
    public ListResponse list(User user) {
        if (Roles.canViewAll(user.getRole())) {
            List<Unavailability> all =
                    unavailabilityRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, LIST_LIMIT));
            List<UnavailabilityView> data = enrichWithUser(all);
            return new ListResponse(data, all.size() >= LIST_LIMIT);
        }
        List<UnavailabilityView> data = enrichWithUser(
                unavailabilityRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        return new ListResponse(data, false);
    }


    @Transactional(readOnly = true)
    public List<UnavailabilityView> mine(User user) {
        return enrichWithUser(unavailabilityRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }


    @Transactional(readOnly = true)
    public List<UnavailabilityView> active(LocalDate today) {
        return enrichWithUser(activeRecords(today));
    }

    private List<Unavailability> activeRecords(LocalDate today) {
        return unavailabilityRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
                        "approved", today, today);
    }


    @Transactional(readOnly = true)
    public List<UnavailabilityView> pending(User user) {
        if (!Roles.isAdmin(user.getRole()) && !Roles.isLider(user.getRole())
                && !Roles.SOCIO.equals(user.getRole())) {
            throw ApiException.forbidden("Acesso restrito a líderes e administradores.");
        }
        List<Unavailability> pendings = unavailabilityRepository.findByStatusOrderByCreatedAtDesc("pending");
        List<Unavailability> visible;
        if (Roles.isLider(user.getRole()) && !Roles.isAdmin(user.getRole())) {
            visible = filterForLider(user, pendings);
        } else {
            visible = pendings;
        }
        List<UnavailabilityView> views = enrichWithUser(visible);
        return attachEventConflicts(views);
    }

    private List<Unavailability> filterForLider(User lider, List<Unavailability> pendings) {
        Member liderMember = lider.getEmail() == null ? null
                : memberRepository.findByEmailIgnoreCase(lider.getEmail().toLowerCase()).orElse(null);
        String liderName = liderMember != null ? liderMember.getName() : null;

        List<Unavailability> out = new ArrayList<>();
        for (Unavailability r : pendings) {
            if (r.getUserId().equals(lider.getId())) {
                continue;
            }
            User requester = userRepository.findById(r.getUserId()).orElse(null);
            if (requester == null) {
                continue;
            }
            if (lider.getDepartment() != null
                    && lider.getDepartment().equals(requester.getDepartment())) {
                out.add(r);
                continue;
            }
            Member requesterMember = resolveMemberForUser(requester);
            if (requesterMember != null && ReportToHelper.reportToMatchesLider(
                    requesterMember.getReportTo(), lider.getEmail(), liderName)) {
                out.add(r);
            }
        }
        return out;
    }

    private Member resolveMemberForUser(User requester) {
        Member m = null;
        if (requester.getMemberId() != null) {
            m = memberRepository.findById(requester.getMemberId()).orElse(null);
        }
        if (m == null && requester.getEmail() != null) {
            m = memberRepository.findByEmailIgnoreCase(requester.getEmail().toLowerCase()).orElse(null);
        }
        return m;
    }


    @Transactional(readOnly = true)
    public ImpactResponse impact(User user, LocalDate today) {
        if (!Roles.canViewAll(user.getRole())) {
            throw ApiException.forbidden("Acesso restrito a administradores.");
        }
        List<Unavailability> activeList = activeRecords(today);
        Map<Long, User> userById = loadUsers(activeList);

        List<UsersOnLeave> onLeave = new ArrayList<>();
        Map<String, DepartmentImpact> deptMap = new LinkedHashMap<>();
        for (Unavailability u : activeList) {
            User ru = userById.get(u.getUserId());
            String name = ru != null && ru.getNome() != null ? ru.getNome() : u.getFullName();
            String dept = u.getDepartment();
            onLeave.add(new UsersOnLeave(u.getUserId(), name, dept,
                    u.getStartDate(), u.getEndDate(), u.getTotalDays()));
            if (dept != null) {
                DepartmentImpact di = deptMap.get(dept);
                List<String> names = di != null ? new ArrayList<>(di.on_leave_names()) : new ArrayList<>();
                if (name != null) {
                    names.add(name);
                }
                deptMap.put(dept, new DepartmentImpact(dept, names.size(), names));
            }
        }
        List<DepartmentImpact> deptImpact = new ArrayList<>(deptMap.values());
        deptImpact.sort((a, b) -> Integer.compare(b.on_leave(), a.on_leave()));
        return new ImpactResponse(onLeave, deptImpact);
    }


    @Transactional
    public void update(User user, Long id, UpdateRequest req, LocalDate today) {
        Unavailability record = unavailabilityRepository.findById(id).orElse(null);
        if (record == null) {
            throw ApiException.notFound("Solicitação não encontrado.");
        }
        if (!"pending".equals(record.getStatus())) {
            throw ApiException.badRequest("Apenas solicitações pendentes podem ser editadas.");
        }
        if (!record.getUserId().equals(user.getId()) && !Roles.isAdminEditor(user.getRole())) {
            throw ApiException.forbidden("Sem permissão para editar esta solicitação.");
        }
        if (req == null || req.start_date() == null || req.end_date() == null) {
            throw ApiException.badRequest("Datas são obrigatórias.");
        }
        LocalDate start = req.start_date();
        LocalDate end = req.end_date();
        if (start.isAfter(end)) {
            throw ApiException.badRequest("Data de início deve ser anterior ao retorno.");
        }
        if (start.isBefore(today.plusDays(15))) {
            throw ApiException.badRequest("A data de início deve ser pelo menos 15 dias a partir de hoje.");
        }
        int expectedDays = BusinessDays.count(start, end);
        if (expectedDays == 0) {
            throw ApiException.badRequest("O período selecionado não contém dias úteis.");
        }
        if (req.department() != null && !setorService.exists(req.department())) {
            throw ApiException.badRequest("Setor inválido.");
        }
        checkOverlap(record.getUserId(), start, end, record.getId());

        record.setStartDate(start);
        record.setEndDate(end);
        record.setTotalDays(expectedDays);
        if (req.unavailability_type() != null && VALID_TYPES.contains(req.unavailability_type())) {
            record.setUnavailabilityType(req.unavailability_type());
        }
        if (req.department() != null) {
            record.setDepartment(req.department());
        }
        unavailabilityRepository.save(record);
    }


    @Transactional
    public void delete(User user, Long id) {
        Unavailability record = unavailabilityRepository.findById(id).orElse(null);
        if (record == null) {
            throw ApiException.notFound("Solicitação não encontrado.");
        }
        if (!Roles.isAdminEditor(user.getRole())) {
            if (!"pending".equals(record.getStatus())) {
                throw ApiException.badRequest("Apenas solicitações pendentes podem ser canceladas.");
            }
            if (!record.getUserId().equals(user.getId())) {
                throw ApiException.forbidden("Sem permissão para excluir esta solicitação.");
            }
        }
        unavailabilityRepository.deleteById(id);
    }


    @Transactional
    public void approve(User approver, Long id) {
        Unavailability record = loadPendingForReview(approver, id, "aprovar");
        record.setStatus("approved");
        record.setReviewedBy(approver.getId());
        record.setReviewedAt(java.time.OffsetDateTime.now());
        unavailabilityRepository.save(record);
        audit(record.getId(), "approved", approver, "Solicitação aprovada.");
    }


    @Transactional
    public void reject(User approver, Long id) {
        Unavailability record = loadPendingForReview(approver, id, "rejeitar");
        record.setStatus("rejected");
        record.setReviewedBy(approver.getId());
        record.setReviewedAt(java.time.OffsetDateTime.now());
        unavailabilityRepository.save(record);
        audit(record.getId(), "rejected", approver, "Solicitação rejeitada.");
    }


    @Transactional
    public void cancelOrShorten(User actor, Long id, LocalDate newEndDate, LocalDate today) {
        if (!Roles.isAdminEditor(actor.getRole()) && !Roles.isLider(actor.getRole())) {
            throw ApiException.forbidden("Apenas administradores ou líderes podem ajustar este período.");
        }
        Unavailability record = unavailabilityRepository.findById(id).orElse(null);
        if (record == null) {
            throw ApiException.notFound("Solicitação não encontrado.");
        }
        if (!"approved".equals(record.getStatus())) {
            throw ApiException.badRequest("Apenas solicitações aprovadas podem ser canceladas ou encurtadas.");
        }
        if (Roles.isLider(actor.getRole()) && !Roles.isAdminEditor(actor.getRole()) && !canApprove(actor, record)) {
            throw ApiException.forbidden("Você não tem permissão para ajustar esta solicitação.");
        }

        String ownerEmail = ownerEmail(record.getUserId());
        String detalhe;
        String assunto;
        String corpo;

        if (newEndDate == null) {
            record.setStatus("canceled");
            detalhe = "Período cancelado por " + actor.getNome() + ".";
            assunto = "Sua indisponibilidade foi cancelada";
            corpo = "Olá " + record.getFullName() + ",\n\n"
                    + "Seu período de indisponibilidade (" + record.getStartDate() + " a "
                    + record.getEndDate() + ") foi CANCELADO por " + actor.getNome() + ".\n"
                    + "Os dias voltam para sua cota. Em caso de dúvida, procure seu líder.\n";
            unavailabilityRepository.save(record);
            audit(record.getId(), "canceled", actor, detalhe);
        } else {
            if (newEndDate.isBefore(record.getStartDate())) {
                throw ApiException.badRequest("A nova data de retorno não pode ser anterior ao início.");
            }
            if (!newEndDate.isBefore(record.getEndDate())) {
                throw ApiException.badRequest("A nova data de retorno deve ser anterior ao fim atual.");
            }
            LocalDate fimAntigo = record.getEndDate();
            int novosDias = BusinessDays.count(record.getStartDate(), newEndDate);
            record.setEndDate(newEndDate);
            record.setTotalDays(novosDias);
            detalhe = "Retorno antecipado: fim alterado de " + fimAntigo + " para " + newEndDate
                    + " (" + novosDias + " dias úteis).";
            assunto = "Seu retorno foi antecipado";
            corpo = "Olá " + record.getFullName() + ",\n\n"
                    + "Seu período de indisponibilidade foi AJUSTADO por " + actor.getNome() + ".\n"
                    + "Nova data de retorno: " + newEndDate + " (antes era " + fimAntigo + ").\n"
                    + "Total de dias úteis agora: " + novosDias + ". O excedente volta para sua cota.\n";
            unavailabilityRepository.save(record);
            audit(record.getId(), "shortened", actor, detalhe);
        }

        emailService.send(ownerEmail, assunto, corpo);
    }


    @Transactional(readOnly = true)
    public List<UnavailabilityAudit> history(User user, Long id) {
        Unavailability record = unavailabilityRepository.findById(id).orElse(null);
        if (record == null) {
            throw ApiException.notFound("Solicitação não encontrado.");
        }
        boolean dono = record.getUserId().equals(user.getId());
        if (!dono && !Roles.canViewAll(user.getRole()) && !Roles.isLider(user.getRole())) {
            throw ApiException.forbidden("Sem permissão para ver o histórico desta solicitação.");
        }
        return auditRepository.findByUnavailabilityIdOrderByCreatedAtAsc(id);
    }

    private Unavailability loadPendingForReview(User approver, Long id, String verb) {
        Unavailability record = unavailabilityRepository.findById(id).orElse(null);
        if (record == null) {
            throw ApiException.notFound("Solicitação não encontrado.");
        }
        if (!canApprove(approver, record)) {
            throw ApiException.forbidden("Você não tem permissão para " + verb + " esta solicitação.");
        }
        if (!"pending".equals(record.getStatus())) {
            throw ApiException.badRequest("Solicitação já foi revisada.");
        }
        return record;
    }


    private boolean canApprove(User approver, Unavailability record) {
        if (Roles.isMasterAdmin(approver.getRole())) {
            return true;
        }
        if (approver.getId().equals(record.getUserId())) {
            return false;
        }
        if (Roles.isAdminEditor(approver.getRole())) {
            return true;
        }
        if (Roles.isLider(approver.getRole())) {
            User requester = userRepository.findById(record.getUserId()).orElse(null);
            if (requester == null) {
                return false;
            }
            if (approver.getDepartment() != null
                    && approver.getDepartment().equals(requester.getDepartment())) {
                return true;
            }
            if (approver.getEmail() != null) {
                Member requesterMember = resolveMemberForUser(requester);
                if (requesterMember != null && requesterMember.getReportTo() != null) {
                    Member liderMember = memberRepository
                            .findByEmailIgnoreCase(approver.getEmail().toLowerCase()).orElse(null);
                    String liderName = liderMember != null ? liderMember.getName() : null;
                    return ReportToHelper.reportToMatchesLider(
                            requesterMember.getReportTo(), approver.getEmail(), liderName);
                }
            }
        }
        return false;
    }


    @Transactional(readOnly = true)
    public int getUsedDaysByUser(Long userId, Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        LocalDate yearStart = LocalDate.of(y, 1, 1);
        LocalDate yearEnd = LocalDate.of(y, 12, 31);
        return unavailabilityRepository
                .findByUserIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        userId, "approved", yearEnd, yearStart)
                .stream()
                .mapToInt(u -> u.getTotalDays() != null ? u.getTotalDays() : 0)
                .sum();
    }



    private Map<Long, User> loadUsers(List<Unavailability> records) {
        Set<Long> ids = new HashSet<>();
        records.forEach(r -> ids.add(r.getUserId()));
        Map<Long, User> map = new HashMap<>();
        if (!ids.isEmpty()) {
            userRepository.findAllById(ids).forEach(u -> map.put(u.getId(), u));
        }
        return map;
    }

    private List<UnavailabilityView> enrichWithUser(List<Unavailability> records) {
        Map<Long, User> userById = loadUsers(records);
        List<UnavailabilityView> out = new ArrayList<>();
        for (Unavailability u : records) {
            User ru = userById.get(u.getUserId());
            out.add(new UnavailabilityView(
                    u.getId(), u.getUserId(), u.getFullName(),
                    ru != null ? ru.getNome() : null,
                    ru != null ? ru.getEmail() : null,
                    ru != null ? ru.getRole() : null,
                    u.getUnavailabilityType(), u.getDepartment(), u.getStartDate(), u.getEndDate(),
                    u.getTotalDays(), u.getStatus(), u.getReviewedBy(), u.getReviewedAt(),
                    u.getCreatedAt(), null));
        }
        return out;
    }


    private List<UnavailabilityView> attachEventConflicts(List<UnavailabilityView> views) {
        if (views.isEmpty()) {
            return views;
        }
        Map<Long, List<EventConflict>> byRequest = computeEventConflicts(views);
        List<UnavailabilityView> out = new ArrayList<>();
        for (UnavailabilityView v : views) {
            List<EventConflict> conflicts = byRequest.getOrDefault(v.id(), List.of());
            out.add(new UnavailabilityView(
                    v.id(), v.user_id(), v.full_name(), v.user_name(), v.user_email(), v.user_role(),
                    v.unavailability_type(), v.department(), v.start_date(), v.end_date(),
                    v.total_days(), v.status(), v.reviewed_by(), v.reviewed_at(), v.created_at(),
                    conflicts));
        }
        return out;
    }

    private Map<Long, List<EventConflict>> computeEventConflicts(List<UnavailabilityView> views) {
        Set<Long> userIds = new HashSet<>();
        views.forEach(v -> userIds.add(v.user_id()));
        Map<Long, Set<Long>> clientesByUser = new HashMap<>();
        for (UserCliente uc : userClienteRepository.findByIdUserIdInAndAtivoTrue(new ArrayList<>(userIds))) {
            clientesByUser.computeIfAbsent(uc.getId().getUserId(), k -> new HashSet<>())
                    .add(uc.getId().getClienteId());
        }
        if (clientesByUser.isEmpty()) {
            return Map.of();
        }

        LocalDate minStart = views.stream().map(UnavailabilityView::start_date)
                .min(LocalDate::compareTo).orElse(null);
        LocalDate maxEnd = views.stream().map(UnavailabilityView::end_date)
                .max(LocalDate::compareTo).orElse(null);
        if (minStart == null || maxEnd == null) {
            return Map.of();
        }

        List<Evento> eventos = eventoRepository
                .findByDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioAsc(maxEnd, minStart);
        if (eventos.isEmpty()) {
            return Map.of();
        }

        List<Long> eventoIds = eventos.stream().map(Evento::getId).toList();
        Map<Long, Set<Long>> evClientesByEvento = new HashMap<>();
        for (EventoCliente ec : eventoClienteRepository.findByIdEventoIdIn(eventoIds)) {
            evClientesByEvento.computeIfAbsent(ec.getId().getEventoId(), k -> new HashSet<>())
                    .add(ec.getId().getClienteId());
        }

        Set<Long> allClienteIds = new HashSet<>();
        clientesByUser.values().forEach(allClienteIds::addAll);
        evClientesByEvento.values().forEach(allClienteIds::addAll);
        Map<Long, String> clienteNomeById = new HashMap<>();
        if (!allClienteIds.isEmpty()) {
            for (Cliente c : clienteRepository.findAllById(allClienteIds)) {
                clienteNomeById.put(c.getId(), c.getNome());
            }
        }

        Map<Long, List<EventConflict>> out = new HashMap<>();
        for (UnavailabilityView req : views) {
            Set<Long> userClientes = clientesByUser.get(req.user_id());
            if (userClientes == null || userClientes.isEmpty()) {
                continue;
            }
            List<EventConflict> conflicts = new ArrayList<>();
            for (Evento ev : eventos) {
                boolean overlap = !ev.getDataInicio().isAfter(req.end_date())
                        && !ev.getDataFim().isBefore(req.start_date());
                if (!overlap) {
                    continue;
                }
                Set<Long> evClientes = evClientesByEvento.getOrDefault(ev.getId(), Set.of());
                List<String> matched = new ArrayList<>();
                for (Long cid : evClientes) {
                    if (userClientes.contains(cid)) {
                        String nome = clienteNomeById.get(cid);
                        if (nome != null) {
                            matched.add(nome);
                        }
                    }
                }
                if (!matched.isEmpty()) {
                    conflicts.add(new EventConflict(ev.getId(), ev.getNome(), ev.getDescricao(),
                            ev.getDataInicio(), ev.getDataFim(), matched));
                }
            }
            if (!conflicts.isEmpty()) {
                out.put(req.id(), conflicts);
            }
        }
        return out;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
