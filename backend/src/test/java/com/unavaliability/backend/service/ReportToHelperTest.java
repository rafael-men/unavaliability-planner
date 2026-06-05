package com.unavaliability.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportToHelper — parse e match de report_to")
class ReportToHelperTest {

    @Test
    @DisplayName("parseReportTo separa por vírgula/ponto-e-vírgula, trim e lowercase")
    void parse() {
        assertThat(ReportToHelper.parseReportTo("A@x.com, B@x.com ; c@x.com"))
                .containsExactly("a@x.com", "b@x.com", "c@x.com");
    }

    @Test
    @DisplayName("parseReportTo de null/vazio retorna lista vazia")
    void parseVazio() {
        assertThat(ReportToHelper.parseReportTo(null)).isEmpty();
        assertThat(ReportToHelper.parseReportTo("   ")).isEmpty();
    }

    @Test
    @DisplayName("match por e-mail do líder (case-insensitive)")
    void matchPorEmail() {
        assertThat(ReportToHelper.reportToMatchesLider("lider@x.com", "LIDER@x.com", null)).isTrue();
    }

    @Test
    @DisplayName("match por nome do líder quando report_to usa nome")
    void matchPorNome() {
        assertThat(ReportToHelper.reportToMatchesLider("João Silva", "outro@x.com", "joão silva")).isTrue();
    }

    @Test
    @DisplayName("não dá match quando não bate e-mail nem nome")
    void semMatch() {
        assertThat(ReportToHelper.reportToMatchesLider("alguem@x.com", "lider@x.com", "Líder")).isFalse();
    }

    @Test
    @DisplayName("report_to vazio nunca dá match")
    void reportToVazio() {
        assertThat(ReportToHelper.reportToMatchesLider(null, "lider@x.com", "Líder")).isFalse();
    }
}
