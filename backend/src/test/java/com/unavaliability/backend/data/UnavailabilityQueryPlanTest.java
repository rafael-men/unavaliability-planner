package com.unavaliability.backend.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Verificação da Cobertura de Índices das Consultas Críticas (V9)")
class UnavailabilityQueryPlanTest {

    private String carregarSqlV9() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V9__composite_indexes.sql")) {
            assertThat(in).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        }
    }

    @Test
    @DisplayName("Garante que a query de calendário/analytics (status + start_date + end_date) tem índice correspondente")
    void deveCobrirFiltroStatusEIntervaloDatas() throws IOException {
        String v9Sql = carregarSqlV9();
        assertThat(v9Sql)
                .contains("idx_unav_status_dates")
                .contains("(status, start_date, end_date)");
    }

    @Test
    @DisplayName("Garante que a verificação de sobreposição (user_id + status) tem índice correspondente")
    void deveCobrirFiltroUserIdEStatus() throws IOException {
        String v9Sql = carregarSqlV9();
        assertThat(v9Sql)
                .contains("idx_unav_user_status")
                .contains("(user_id, status)");
    }

    @Test
    @DisplayName("Garante que a paginação por data de criação tem índice descendente correspondente")
    void deveCobrirOrdenacaoCreatedAtDesc() throws IOException {
        String v9Sql = carregarSqlV9();
        assertThat(v9Sql)
                .contains("idx_unav_created_at_desc")
                .contains("(created_at desc)");
    }

    @Test
    @DisplayName("Garante que a hierarquia de liderança em members tem índice em report_to")
    void deveCobrirHierarquiaMembersReportTo() throws IOException {
        String v9Sql = carregarSqlV9();
        assertThat(v9Sql)
                .contains("idx_members_report_to")
                .contains("(report_to)");
    }

    @Test
    @DisplayName("Garante que a tabela de eventos tem índice cobrindo intervalo de datas")
    void deveCobrirEventosPorDatas() throws IOException {
        String v9Sql = carregarSqlV9();
        assertThat(v9Sql)
                .contains("idx_eventos_dates")
                .contains("(data_inicio, data_fim)");
    }
}
