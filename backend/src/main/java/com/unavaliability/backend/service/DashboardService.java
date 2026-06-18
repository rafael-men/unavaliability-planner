package com.unavaliability.backend.service;

import com.unavaliability.backend.dto.UnavailabilityDtos.UnavailabilityView;
import com.unavaliability.backend.domain.Status;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final UnavailabilityService unavailabilityService;

    public DashboardService(UserRepository userRepository, UnavailabilityService unavailabilityService) {
        this.userRepository = userRepository;
        this.unavailabilityService = unavailabilityService;
    }

    @Transactional(readOnly = true)
    public Object dashboard(User user, LocalDate today) {
        if (Roles.canViewAll(user.getRole())) {
            List<User> pendingUsers = userRepository.findByStatus(Status.UserAccount.PENDING);
            List<UnavailabilityView> pendingUnavail = unavailabilityService.pending(user);
            List<UnavailabilityView> active = unavailabilityService.active(today);
            return new AdminDashboard(user.getRole(), pendingUsers, pendingUnavail, active,
                    Roles.isAdminEditor(user.getRole()));
        }
        return new UserDashboard(user.getRole(), unavailabilityService.mine(user));
    }

    public record AdminDashboard(
            String role,
            List<User> pending_users,
            List<UnavailabilityView> pending_unavailability,
            List<UnavailabilityView> active_unavailability,
            boolean is_editor) {
    }

    public record UserDashboard(String role, List<UnavailabilityView> my_unavailability) {
    }
}
