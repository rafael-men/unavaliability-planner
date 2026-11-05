package com.unavaliability.backend.service;

import com.unavaliability.backend.models.Unavailability;
import com.unavaliability.backend.models.User;
import com.unavaliability.backend.repositories.UnavailabilityRepository;
import com.unavaliability.backend.repositories.UserRepository;
import com.unavaliability.backend.security.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    UnavailabilityRepository unavailabilityRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AuthorizationService authorizationService;

    private ExportService service;

    @BeforeEach
    void setUp() {
        service = new ExportService(
                unavailabilityRepository,
                userRepository,
                authorizationService);
    }

    @Test
    void csvPrefixesFormulaInjectionCharacters() {
        User actor = new User();
        actor.setId(1L);
        actor.setRole("admin_leitor");

        Unavailability record = new Unavailability();
        record.setId(10L);
        record.setUserId(2L);
        record.setFullName("@fallback");
        record.setDepartment("+setor");
        record.setUnavailabilityType("-tipo");
        record.setStatus("=pending");

        User requester = new User();
        requester.setId(2L);
        requester.setNome("=HYPERLINK(\"http://malicioso.test\")");

        when(unavailabilityRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(List.of(record));
        when(userRepository.findAllById(any()))
                .thenReturn(List.of(requester));

        ExportService.Arquivo arquivo =
                service.exportarRelatorio(actor, ExportService.Formato.CSV);

        String csv = new String(arquivo.conteudo(), StandardCharsets.UTF_8);

        assertThat(csv).contains("'=HYPERLINK(\"\"http://malicioso.test\"\")");
        assertThat(csv).contains("'+setor");
        assertThat(csv).contains("'-tipo");
        assertThat(csv).contains("'=pending");
        assertThat(csv).doesNotContain(
                "\"=HYPERLINK(\"\"http://malicioso.test\"\")");
    }

    @Test
    void csvPrefixesTabAndCarriageReturn() {
        User actor = new User();
        actor.setRole("admin_leitor");

        Unavailability tabRecord = new Unavailability();
        tabRecord.setId(11L);
        tabRecord.setUserId(3L);
        tabRecord.setFullName("\tformula");

        Unavailability carriageReturnRecord = new Unavailability();
        carriageReturnRecord.setId(12L);
        carriageReturnRecord.setUserId(4L);
        carriageReturnRecord.setFullName("\rformula");

        when(unavailabilityRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(List.of(tabRecord, carriageReturnRecord));
        when(userRepository.findAllById(any()))
                .thenReturn(List.of());

        ExportService.Arquivo arquivo =
                service.exportarRelatorio(actor, ExportService.Formato.CSV);

        String csv = new String(arquivo.conteudo(), StandardCharsets.UTF_8);

        assertThat(csv).contains("'\tformula");
        assertThat(csv).contains("'\rformula");
    }

    @Test
    void csvDoesNotChangeSafeValues() {
        User actor = new User();
        actor.setRole("admin_leitor");

        Unavailability record = new Unavailability();
        record.setId(13L);
        record.setUserId(5L);
        record.setFullName("Ana Silva");
        record.setDepartment("Tecnologia");

        when(unavailabilityRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(List.of(record));
        when(userRepository.findAllById(any()))
                .thenReturn(List.of());

        ExportService.Arquivo arquivo =
                service.exportarRelatorio(actor, ExportService.Formato.CSV);

        String csv = new String(arquivo.conteudo(), StandardCharsets.UTF_8);

        assertThat(csv).contains("Ana Silva");
        assertThat(csv).contains("Tecnologia");
        assertThat(csv).doesNotContain("'Ana Silva");
        assertThat(csv).doesNotContain("'Tecnologia");
    }
}
