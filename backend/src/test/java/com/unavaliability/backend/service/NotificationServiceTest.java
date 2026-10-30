package com.unavaliability.backend.service;

import com.unavaliability.backend.domain.Status;
import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock UserRepository userRepository;
    @Mock UnavailabilityService unavailabilityService;
    @Mock MemberRepository memberRepository;
    @Mock UnavailabilityRepository unavailabilityRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(
                userRepository,
                unavailabilityService,
                memberRepository,
                unavailabilityRepository);
    }

    @Test
    void countForAdminEditorUsesOnlyCountQueries() {
        User user = user(1L, "admin_editor");
        when(userRepository.countByStatus(Status.UserAccount.PENDING)).thenReturn(2L);
        when(unavailabilityRepository.countByStatus(Status.Unavailability.PENDING)).thenReturn(3L);

        NotificationService.NotificationCountResponse response = service.count(user);

        assertThat(response.count()).isEqualTo(5L);
        verify(userRepository).countByStatus(Status.UserAccount.PENDING);
        verify(unavailabilityRepository).countByStatus(Status.Unavailability.PENDING);
        verify(userRepository, never()).findByStatus(Status.UserAccount.PENDING);
        verify(unavailabilityService, never()).pendingForNotifications(user);
    }

    @Test
    void countForLeaderUsesLeaderScopedCountQuery() {
        User user = user(7L, "lider");
        user.setMemberId(7L);
        user.setEmail("leader@example.com");
        user.setDepartment("Tecnologia");
        Member member = new Member();
        member.setName("Líder");
        when(memberRepository.findById(7L)).thenReturn(Optional.of(member));
        when(unavailabilityRepository.countPendingVisibleToLeader(
                Status.Unavailability.PENDING,
                7L,
                "Tecnologia",
                "leader@example.com",
                "Líder")).thenReturn(4L);

        NotificationService.NotificationCountResponse response = service.count(user);

        assertThat(response.count()).isEqualTo(4L);
        verify(unavailabilityRepository).countPendingVisibleToLeader(
                Status.Unavailability.PENDING,
                7L,
                "Tecnologia",
                "leader@example.com",
                "Líder");
        verify(unavailabilityRepository, never()).countByStatus(Status.Unavailability.PENDING);
    }

    @Test
    void listUsesNotificationPathWithoutEventConflictCalculation() {
        User user = user(1L, "admin_leitor");
        when(unavailabilityService.pendingForNotifications(user)).thenReturn(java.util.List.of());

        NotificationService.NotificationsResponse response = service.list(user);

        assertThat(response.items()).isEmpty();
        verify(unavailabilityService).pendingForNotifications(user);
        verify(unavailabilityService, never()).pending(user);
    }

    private static User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
