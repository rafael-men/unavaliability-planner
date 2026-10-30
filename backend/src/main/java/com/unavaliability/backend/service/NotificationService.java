package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.UnavailabilityDtos.UnavailabilityView;
import com.unavaliability.backend.domain.Status;
import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NotificationService {

    private final UserRepository userRepository;
    private final UnavailabilityService unavailabilityService;
    private final MemberRepository memberRepository;
    private final UnavailabilityRepository unavailabilityRepository;

    public NotificationService(
            UserRepository userRepository,
            UnavailabilityService unavailabilityService,
            MemberRepository memberRepository,
            UnavailabilityRepository unavailabilityRepository) {
        this.userRepository = userRepository;
        this.unavailabilityService = unavailabilityService;
        this.memberRepository = memberRepository;
        this.unavailabilityRepository = unavailabilityRepository;
    }

    @Transactional(readOnly = true)
    public NotificationsResponse list(User user) {
        List<NotificationItem> items = new ArrayList<>();

        if (Roles.isAdminEditor(user.getRole())) {
            for (User u : userRepository.findByStatus(Status.UserAccount.PENDING)) {
                String title = u.getNome() != null ? u.getNome() : u.getEmail();
                items.add(new NotificationItem(
                        "user-" + u.getId(), "pending_user", title,
                        "Novo cadastro · " + u.getEmail(), u.getCreatedAt(), "/admin/users"));
            }
        }

        if (Roles.isAdmin(user.getRole()) || Roles.isLider(user.getRole())
                || Roles.SOCIO.equals(user.getRole())) {

            for (UnavailabilityView r : unavailabilityService.pendingForNotifications(user)) {
                String title = r.user_name() != null ? r.user_name()
                        : (r.full_name() != null ? r.full_name() : "Solicitação");
                items.add(new NotificationItem(
                        "unavail-" + r.id(), "pending_unavailability", title,
                        "Indisponibilidade · " + r.start_date() + " a " + r.end_date(),
                        r.created_at(), "/unavailability"));
            }
        }

        items.sort(Comparator.comparing(
                NotificationItem::created_at,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return new NotificationsResponse(items, items.size());
    }

    //Respostas para contagem de notificações pendentes

    @Transactional(readOnly = true)
    public NotificationCountResponse count(User user) {
        long count = 0;
        if (Roles.isAdminEditor(user.getRole())) {
            count += userRepository.countByStatus(Status.UserAccount.PENDING);
        }
        if (Roles.isAdmin(user.getRole()) || Roles.isLider(user.getRole())
                || Roles.SOCIO.equals(user.getRole())) {
            if (Roles.isLider(user.getRole()) && !Roles.isAdmin(user.getRole())) {
                Member leader = null;
                if (user.getMemberId() != null) {
                    leader = memberRepository.findById(user.getMemberId()).orElse(null);
                }
                if (leader == null && user.getEmail() != null) {
                    leader = memberRepository.findByEmailIgnoreCase(user.getEmail().toLowerCase()).orElse(null);
                }

                String leaderName = leader != null ? leader.getName() : null;
                count += unavailabilityRepository.countPendingVisibleToLeader(
                        Status.Unavailability.PENDING,
                        user.getId(),
                        user.getDepartment(),
                        user.getEmail(),
                        leaderName);
            } else {
                count += unavailabilityRepository.countByStatus(
                        Status.Unavailability.PENDING);
            }
        }
        return new NotificationCountResponse(count);
    }

    public record NotificationItem(
            String id, String type, String title, String subtitle,
            OffsetDateTime created_at, String href) {
    }

    public record NotificationsResponse(List<NotificationItem> items, int count) {
    }

    public record NotificationCountResponse(long count) {
    }
}