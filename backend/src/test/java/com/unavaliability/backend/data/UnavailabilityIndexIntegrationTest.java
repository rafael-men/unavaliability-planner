package com.unavaliability.backend.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validação dos Índices da Migration V9")
class UnavailabilityIndexIntegrationTest {

    private String carregarSqlV9() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V9__composite_indexes.sql")) {
            assertThat(in)
                    .as("O arquivo V9__composite_indexes.sql deve estar presente em src/main/resources/db/migration/")
                    .isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        }
    }

    @Test
    @DisplayName("V9 cria índice composto para status e intervalo de datas em unavailability")
    void deveConterIndiceStatusDatas() throws IOException {
        String sql = carregarSqlV9();
        assertThat(sql)
                .contains("create index if not exists idx_unav_status_dates")
                .contains("on unavailability")
                .contains("status, start_date, end_date");
    }

    @Test
    @DisplayName("V9 cria índice composto para validação de sobreposição por user_id e status")
    void deveConterIndiceUserStatus() throws IOException {
        String sql = carregarSqlV9();
        assertThat(sql)
                .contains("create index if not exists idx_unav_user_status")
                .contains("on unavailability")
                .contains("user_id, status");
    }

    @Test
    @DisplayName("V9 cria índice ordenado por created_at DESC para listagens paginadas")
    void deveConterIndiceCreatedAtDesc() throws IOException {
        String sql = carregarSqlV9();
        assertThat(sql)
                .contains("create index if not exists idx_unav_created_at_desc")
                .contains("on unavailability")
                .contains("created_at desc");
    }

    @Test
    @DisplayName("V9 cria índice em members(report_to) para otimizar busca de subordinados")
    void deveConterIndiceMembersReportTo() throws IOException {
        String sql = carregarSqlV9();
        assertThat(sql)
                .contains("create index if not exists idx_members_report_to")
                .contains("on members")
                .contains("report_to");
    }

    @Test
    @DisplayName("V9 cria índice composto de datas em eventos")
    void deveConterIndiceEventosDates() throws IOException {
        String sql = carregarSqlV9();
        assertThat(sql)
                .contains("create index if not exists idx_eventos_dates")
                .contains("on eventos")
                .contains("data_inicio, data_fim");
    }
}
