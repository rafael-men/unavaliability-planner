package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.UnavailabilityDtos.UnavailabilityView;
import com.unavaliability.backend.models.User;
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

    public NotificationService(UserRepository userRepository, UnavailabilityService unavailabilityService) {
        this.userRepository = userRepository;
        this.unavailabilityService = unavailabilityService;
    }

    @Transactional(readOnly = true)
    public NotificationsResponse list(User user) {
        List<NotificationItem> items = new ArrayList<>();

        if (Roles.isAdminEditor(user.getRole())) {
            for (User u : userRepository.findByStatus("pending")) {
                String title = u.getNome() != null ? u.getNome() : u.getEmail();
                items.add(new NotificationItem(
                        "user-" + u.getId(), "pending_user", title,
                        "Novo cadastro · " + u.getEmail(), u.getCreatedAt(), "/admin/users"));
            }
        }

        if (Roles.isAdmin(user.getRole()) || Roles.isLider(user.getRole())
                || Roles.SOCIO.equals(user.getRole())) {
            for (UnavailabilityView r : unavailabilityService.pending(user)) {
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

    public record NotificationItem(
            String id, String type, String title, String subtitle,
            OffsetDateTime created_at, String href) {
    }

    public record NotificationsResponse(List<NotificationItem> items, int count) {
    }
}
