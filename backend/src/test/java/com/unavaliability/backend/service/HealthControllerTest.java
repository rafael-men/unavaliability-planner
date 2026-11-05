package com.unavaliability.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import com.unavaliability.backend.controllers.HealthController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    private JdbcTemplate jdbcTemplate;
    private HealthController controller;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        controller = new HealthController(jdbcTemplate);
    }

    @Test
    void retornaOkQuandoBancoResponde() {
        when(jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class))
                .thenReturn(1);

        var response = controller.health();

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .containsEntry("status", "ok")
                .containsEntry("database", "ok");
    }

    @Test
    void retorna503QuandoBancoFalha() {
        when(jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class))
                .thenThrow(new DataAccessResourceFailureException(
                        "Connection refused"));

        var response = controller.health();

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        assertThat(response.getBody())
                .containsEntry("status", "error")
                .containsEntry("database", "unavailable");
    }

    @Test
    void retorna503QuandoBancoRetornaValorInesperado() {
        when(jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class))
                .thenReturn(0);

        var response = controller.health();

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        assertThat(response.getBody())
                .containsEntry("status", "error")
                .containsEntry("database", "unavailable");
    }
}