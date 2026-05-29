package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.UserDtos.AssignSetorRequest;
import com.unavaliability.backend.dto.UserDtos.ChangeRoleRequest;
import com.unavaliability.backend.dto.UserDtos.CreateUserRequest;
import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.Unavailability;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.Roles;
import com.unavaliability.backend.util.TextUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class UserAdminService {

    private static final Set<String> ROLES_ABOVE_LIDER =
            Set.of(Roles.ADMIN_MASTER, Roles.ADMIN_EDITOR, Roles.ADMIN_LEITOR, Roles.SOCIO);

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final UnavailabilityRepository unavailabilityRepository;
    private final PasswordEncoder passwordEncoder;
    private final SetorService setorService;

    public UserAdminService(UserRepository userRepository, MemberRepository memberRepository,
                            UnavailabilityRepository unavailabilityRepository,
                            PasswordEncoder passwordEncoder, SetorService setorService) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.passwordEncoder = passwordEncoder;
        this.setorService = setorService;
    }


    private void requireAdminOnly(User actor) {
        if (!Roles.isAdmin(actor.getRole())) {
            throw ApiException.forbidden("Acesso restrito a administradores.");
        }
    }

    private void requireAdminEditor(User actor) {
        if (!Roles.isAdminEditor(actor.getRole())) {
            throw ApiException.forbidden("Apenas Admin Editor pode realizar esta ação.");
        }
    }

    private void requireMasterAdmin(User actor) {
        if (!Roles.isMasterAdmin(actor.getRole())) {
            throw ApiException.forbidden("Acesso exclusivo do Admin Master.");
        }
    }


    @Transactional(readOnly = true)
    public List<User> listAll(User actor) {
        requireAdminOnly(actor);
        return userRepository.findAll();
    }


    @Transactional(readOnly = true)
    public List<User> listPending(User actor) {
        requireAdminOnly(actor);
        return userRepository.findByStatus("pending");
    }


    @Transactional
    public User createUser(User actor, CreateUserRequest req) {
        requireMasterAdmin(actor);
        if (req == null || isBlank(req.email()) || isBlank(req.password())
                || isBlank(req.full_name()) || isBlank(req.department()) || isBlank(req.role())) {
            throw ApiException.badRequest("Todos os campos são obrigatórios.");
        }
        String emailLower = req.email().toLowerCase().trim();
        if (!TextUtils.isValidEmail(emailLower)) {
            throw ApiException.badRequest("Email inválido.");
        }
        if (!setorService.exists(req.department())) {
            throw ApiException.badRequest("Setor inválido.");
        }
        if (!Roles.ASSIGNABLE_ROLES.contains(req.role())) {
            throw ApiException.badRequest("Role inválido.");
        }
        if (req.password().length() < 6) {
            throw ApiException.badRequest("Senha deve ter pelo menos 6 caracteres.");
        }
        if (userRepository.existsByEmailIgnoreCase(emailLower)) {
            throw ApiException.badRequest("Este email já está cadastrado.");
        }

        Member member = memberRepository.findByEmailIgnoreCase(emailLower).orElse(null);
        User user = new User();
        user.setEmail(emailLower);
        user.setPassw(passwordEncoder.encode(req.password()));
        user.setNome(TextUtils.cleanText(req.full_name()));
        user.setDepartment(req.department());
        user.setMemberId(member != null ? member.getId() : null);
        user.setRole(req.role());
        user.setStatus("approved");
        user.setApprovedBy(actor.getId());
        user.setApprovedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }


    @Transactional
    public void deleteUser(User actor, Long userId) {
        requireAdminEditor(actor);
        User target = userRepository.findById(userId).orElse(null);
        if (target == null) {
            throw ApiException.notFound("Usuário não encontrado.");
        }
        if (target.getId().equals(actor.getId())) {
            throw ApiException.badRequest("Você não pode remover a si mesmo.");
        }
        if (Roles.isMasterAdmin(target.getRole())) {
            throw ApiException.forbidden("O Admin Master não pode ser removido.");
        }
        List<Unavailability> active =
                unavailabilityRepository.findByUserIdAndStatusIn(userId, List.of("pending", "approved"));
        if (!active.isEmpty()) {
            String labels = active.stream()
                    .map(u -> u.getStartDate() + " a " + u.getEndDate())
                    .collect(Collectors.joining(", "));
            throw ApiException.badRequest(
                    "Usuário possui solicitações ativas: " + labels
                            + ". Cancele-as antes de remover o usuário.");
        }
        userRepository.deleteById(userId);
    }


    @Transactional
    public void assignSetor(User actor, Long userId, AssignSetorRequest req) {
        requireMasterAdmin(actor);
        String setor = req == null ? null : req.setor();
        if (setor != null && !setor.isBlank() && !setorService.exists(setor)) {
            throw ApiException.badRequest("Setor inválido.");
        }
        User target = userRepository.findById(userId).orElse(null);
        if (target == null) {
            throw ApiException.notFound("Usuário não encontrado.");
        }
        if (Roles.isMasterAdmin(target.getRole())) {
            throw ApiException.forbidden("Não é possível alterar o Admin Master.");
        }
        boolean isLider = req != null && Boolean.TRUE.equals(req.is_lider());
        String newRole;
        if (isLider) {
            newRole = ROLES_ABOVE_LIDER.contains(target.getRole()) ? target.getRole() : Roles.LIDER;
        } else {
            newRole = Roles.LIDER.equals(target.getRole()) ? Roles.COLABORADOR : target.getRole();
        }
        target.setDepartment(setor != null && !setor.isBlank() ? setor : null);
        target.setRole(newRole);
        userRepository.save(target);
    }


    @Transactional
    public void approveUser(User actor, Long userId) {
        requireAdminEditor(actor);
        User target = userRepository.findById(userId).orElse(null);
        if (target == null) {
            throw ApiException.notFound("Usuário não encontrado.");
        }
        if ("approved".equals(target.getStatus())) {
            throw ApiException.badRequest("Usuário já está aprovado.");
        }
        target.setStatus("approved");
        target.setApprovedBy(actor.getId());
        target.setApprovedAt(OffsetDateTime.now());
        userRepository.save(target);
    }


    @Transactional
    public void rejectUser(User actor, Long userId) {
        requireAdminEditor(actor);
        User target = userRepository.findById(userId).orElse(null);
        if (target == null) {
            throw ApiException.notFound("Usuário não encontrado.");
        }
        if ("rejected".equals(target.getStatus())) {
            throw ApiException.badRequest("Usuário já foi rejeitado.");
        }
        target.setStatus("rejected");
        target.setApprovedBy(actor.getId());
        target.setApprovedAt(OffsetDateTime.now());
        userRepository.save(target);
    }


    @Transactional
    public void changeRole(User actor, Long userId, ChangeRoleRequest req) {
        requireAdminEditor(actor);
        String role = req == null ? null : req.role();
        if (role == null || !Roles.ASSIGNABLE_ROLES.contains(role)) {
            throw ApiException.badRequest("Role inválido.");
        }
        if (userId.equals(actor.getId())) {
            throw ApiException.badRequest("Você não pode alterar seu próprio role.");
        }
        User target = userRepository.findById(userId).orElse(null);
        if (target == null) {
            throw ApiException.notFound("Usuário não encontrado.");
        }
        if (Roles.isMasterAdmin(target.getRole())) {
            throw ApiException.forbidden("O role de Admin Master não pode ser alterado.");
        }
        target.setRole(role);
        userRepository.save(target);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
