package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.MemberDtos.MemberRequest;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.AuthorizationService;
import com.unavaliability.backend.util.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final UnavailabilityService unavailabilityService;
    private final AuthorizationService authz;

    public MemberService(MemberRepository memberRepository, UserRepository userRepository,
                         UnavailabilityService unavailabilityService, AuthorizationService authz) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.unavailabilityService = unavailabilityService;
        this.authz = authz;
    }


    @Transactional(readOnly = true)
    public List<Member> listAll() {
        return memberRepository.findAllByOrderByNameAsc();
    }


    @Transactional
    public Member create(User actor, MemberRequest req) {
        authz.requireMasterAdmin(actor);
        validateRequired(req);
        String emailLower = null;
        if (req.email() != null && !req.email().isBlank()) {
            emailLower = req.email().toLowerCase().trim();
            if (!TextUtils.isValidEmail(emailLower)) {
                throw ApiException.badRequest("Email inválido.");
            }
            if (memberRepository.existsByEmailIgnoreCase(emailLower)) {
                throw ApiException.badRequest("Já existe um membro com este email.");
            }
        }
        Member m = new Member();
        applyFields(m, req, emailLower);
        return memberRepository.save(m);
    }


    @Transactional
    public Member update(User actor, Long id, MemberRequest req) {
        authz.requireMasterAdmin(actor);
        Member m = memberRepository.findById(id).orElse(null);
        if (m == null) {
            throw ApiException.notFound("Membro não encontrado.");
        }
        validateRequired(req);
        String emailLower = m.getEmail();
        if (req.email() != null && !req.email().isBlank()) {
            emailLower = req.email().toLowerCase().trim();
            if (!TextUtils.isValidEmail(emailLower)) {
                throw ApiException.badRequest("Email inválido.");
            }
            Member existing = memberRepository.findByEmailIgnoreCase(emailLower).orElse(null);
            if (existing != null && !existing.getId().equals(id)) {
                throw ApiException.badRequest("Já existe outro membro com este email.");
            }
        }
        applyFields(m, req, emailLower);
        return memberRepository.save(m);
    }


    @Transactional
    public void delete(User actor, Long id) {
        authz.requireMasterAdmin(actor);
        if (!memberRepository.existsById(id)) {
            throw ApiException.notFound("Membro não encontrado.");
        }
        memberRepository.deleteById(id);
    }

    private void validateRequired(MemberRequest req) {
        if (req == null || isBlank(req.name()) || isBlank(req.area()) || isBlank(req.funcao())) {
            throw ApiException.badRequest("Nome, área e função são obrigatórios.");
        }
    }

    private void applyFields(Member m, MemberRequest req, String emailLower) {
        m.setName(TextUtils.cleanText(req.name()));
        if (emailLower != null) {
            m.setEmail(emailLower);
        }
        m.setArea(TextUtils.cleanText(req.area()));
        m.setSquad(req.squad() != null ? TextUtils.cleanText(req.squad()) : null);
        m.setFuncao(TextUtils.cleanText(req.funcao()));
        m.setReportTo(req.report_to() != null ? TextUtils.cleanText(req.report_to()) : null);
        m.setOperacoes(TextUtils.toBool(req.operacoes()));
        Integer quota = req.day_offs_quota();
        m.setDayOffsQuota(quota != null && quota != 0 ? quota : 20);
    }


    @Transactional(readOnly = true)
    public MemberMeView me(User user) {
        Member member = user.getEmail() == null ? null
                : memberRepository.findByEmailIgnoreCase(user.getEmail().toLowerCase()).orElse(null);
        if (member == null) {
            return new MemberMeView(null, null, 0, 0, 0);
        }
        int quota = member.getDayOffsQuota() != null ? member.getDayOffsQuota() : 0;
        int used = unavailabilityService.getUsedDaysByUser(user.getId(), null);
        Object approver = resolveApprover(member);
        return new MemberMeView(member, approver, used, Math.max(0, quota - used), quota);
    }


    @Transactional(readOnly = true)
    public MemberByEmailView byEmail(String email) {
        Member member = memberRepository.findByEmailIgnoreCase(email).orElse(null);
        if (member == null) {
            throw ApiException.notFound("Membro não encontrado.");
        }
        int quota = member.getDayOffsQuota() != null ? member.getDayOffsQuota() : 0;
        int used = 0;
        if (member.getEmail() != null) {
            User u = userRepository.findByEmailIgnoreCase(member.getEmail()).orElse(null);
            if (u != null) {
                used = unavailabilityService.getUsedDaysByUser(u.getId(), null);
            }
        }
        return new MemberByEmailView(member, used, Math.max(0, quota - used));
    }


    private Object resolveApprover(Member member) {
        List<String> approverEmails = ReportToHelper.parseReportTo(member.getReportTo()).stream()
                .filter(s -> s.contains("@"))
                .toList();
        if (approverEmails.isEmpty()) {
            return null;
        }
        List<Member> approvers = memberRepository.findByEmailInIgnoreCase(approverEmails);
        if (approvers.isEmpty()) {
            return null;
        }
        List<ApproverRef> refs = new ArrayList<>();
        approvers.forEach(a -> refs.add(new ApproverRef(a.getName(), a.getEmail())));
        return refs.size() == 1 ? refs.get(0) : refs;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public record ApproverRef(String name, String email) {
    }

    public record MemberMeView(Member member, Object approver, int used_days, int remaining_days, int quota) {
    }

    public record MemberByEmailView(Member member, int used_days, int remaining_days) {
    }
}
