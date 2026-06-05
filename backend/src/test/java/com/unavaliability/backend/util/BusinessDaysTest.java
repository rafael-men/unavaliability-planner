package com.unavaliability.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessDays — contagem de dias úteis (seg–sex), inclusiva")
class BusinessDaysTest {

    @Test
    @DisplayName("conta uma semana de trabalho completa = 5")
    void semanaCompleta() {
        int dias = BusinessDays.count(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5));
        assertThat(dias).isEqualTo(5);
    }

    @Test
    @DisplayName("inclui início e fim no mesmo dia útil = 1")
    void mesmoDiaUtil() {
        int dias = BusinessDays.count(LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 3));
        assertThat(dias).isEqualTo(1);
    }

    @Test
    @DisplayName("fim de semana não conta = 0")
    void fimDeSemana() {
        int dias = BusinessDays.count(LocalDate.of(2024, 1, 6), LocalDate.of(2024, 1, 7));
        assertThat(dias).isZero();
    }

    @Test
    @DisplayName("período com fim de semana no meio desconta sáb/dom")
    void periodoComFimDeSemana() {
        int dias = BusinessDays.count(LocalDate.of(2024, 1, 4), LocalDate.of(2024, 1, 9));
        assertThat(dias).isEqualTo(4);
    }

    @Test
    @DisplayName("duas semanas cheias = 10")
    void duasSemanas() {
        int dias = BusinessDays.count(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 12));
        assertThat(dias).isEqualTo(10);
    }
}
