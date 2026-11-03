package com.unavaliability.backend.service;

import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.Member;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.MemberRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceAuthorizationTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    UnavailabilityService unavailabilityService;

    private MemberService service;

    @BeforeEach
    void setUp() {
        service = new MemberService(
                memberRepository,
                userRepository,
                unavailabilityService,
                new AuthorizationService());
    }

    @Test
    void colaboradorNaoPodeListarTodosOsMembros() {
        User actor = user(
                1L,
                "colaborador",
                "colaborador@empresa.com");

        assertThatThrownBy(() -> service.listAll(actor))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("administradores");

        verify(memberRepository, never()).findAllByOrderByNameAsc();
    }

    @Test
    void colaboradorPodeConsultarProprioEmail() {
        User actor = user(
                1L,
                "colaborador",
                "colaborador@empresa.com");

        Member ownMember = member(
                "colaborador@empresa.com",
                "Colaborador",
                null);

        when(memberRepository.findByEmailIgnoreCase(
                "colaborador@empresa.com"))
                .thenReturn(Optional.of(ownMember));

        when(userRepository.findByEmailIgnoreCase(
                "colaborador@empresa.com"))
                .thenReturn(Optional.of(actor));

        when(unavailabilityService.getUsedDaysByUser(
                1L,
                null))
                .thenReturn(2);

        service.byEmail(actor, "colaborador@empresa.com");
    }

    @Test
    void colaboradorNaoPodeConsultarOutroMembro() {
        User actor = user(
                1L,
                "colaborador",
                "colaborador@empresa.com");

        Member other = member(
                "outro@empresa.com",
                "Outro",
                "lider@empresa.com");

        when(memberRepository.findByEmailIgnoreCase(
                "outro@empresa.com"))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() ->
                service.byEmail(actor, "outro@empresa.com"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("próprio perfil");

        verify(userRepository, never())
                .findByEmailIgnoreCase("outro@empresa.com");
    }

    @Test
    void liderPodeConsultarLideradoDireto() {
        User leader = user(
                1L,
                "lider",
                "lider@empresa.com");

        Member leaderMember = member(
                "lider@empresa.com",
                "Líder",
                null);

        Member directReport = member(
                "liderado@empresa.com",
                "Liderado",
                "lider@empresa.com");

        when(memberRepository.findByEmailIgnoreCase(
                "liderado@empresa.com"))
                .thenReturn(Optional.of(directReport));

        when(memberRepository.findByEmailIgnoreCase(
                "lider@empresa.com"))
                .thenReturn(Optional.of(leaderMember));

        when(userRepository.findByEmailIgnoreCase(
                "liderado@empresa.com"))
                .thenReturn(Optional.empty());

        service.byEmail(leader, "liderado@empresa.com");
    }

    @Test
    void administradorPodeConsultarQualquerMembro() {
        User admin = user(
                1L,
                "admin_leitor",
                "admin@empresa.com");

        Member other = member(
                "outro@empresa.com",
                "Outro",
                null);

        when(memberRepository.findByEmailIgnoreCase(
                "outro@empresa.com"))
                .thenReturn(Optional.of(other));

        when(userRepository.findByEmailIgnoreCase(
                "outro@empresa.com"))
                .thenReturn(Optional.empty());

        service.byEmail(admin, "outro@empresa.com");
    }

    private static User user(Long id, String role, String email) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setEmail(email);
        return user;
    }

    private static Member member(
            String email,
            String name,
            String reportTo) {

        Member member = new Member();
        member.setEmail(email);
        member.setName(name);
        member.setReportTo(reportTo);
        member.setDayOffsQuota(20);
        return member;
    }
}