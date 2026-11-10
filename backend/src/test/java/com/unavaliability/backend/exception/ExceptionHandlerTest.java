package com.unavaliability.backend.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.unavaliability.backend.util.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler — logs e status HTTP")
class ExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/eventos");

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void anexaAppender() {
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MDC.put(TraceIdFilter.MDC_KEY, "trace-teste-123");
    }

    @AfterEach
    void removeAppender() {
        logger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    @DisplayName("erro inesperado: 500, mensagem genérica, traceId no corpo e ERROR com stack trace no log")
    void erroInesperadoLogaComStackTrace() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleGeneric(new IllegalStateException("falha de conexão"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody())
                .containsEntry("error", "Erro interno. Tente novamente.")
                .containsEntry("traceId", "trace-teste-123");

        assertThat(appender.list).hasSize(1);
        ILoggingEvent evento = appender.list.get(0);
        assertThat(evento.getLevel()).isEqualTo(Level.ERROR);
        assertThat(evento.getFormattedMessage()).contains("trace-teste-123", "/api/eventos");
        assertThat(evento.getThrowableProxy()).isNotNull();
        assertThat(evento.getThrowableProxy().getMessage()).isEqualTo("falha de conexão");
    }

    @Test
    @DisplayName("acesso negado vira 403 (não 500) e não gera ERROR")
    void acessoNegadoRetorna403() {
        ResponseEntity<Map<String, Object>> resp =
                handler.handleAccessDenied(new AccessDeniedException("sem permissão"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    @DisplayName("rota inexistente vira 404 e não gera ERROR")
    void rotaInexistenteRetorna404() {
        ResponseEntity<Map<String, Object>> resp = handler.handleClientError(
                new NoResourceFoundException(HttpMethod.GET, "/api/nada", null), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("error", "Recurso não encontrado.");
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    @DisplayName("JSON malformado vira 400 e não gera ERROR")
    void jsonMalformadoRetorna400() {
        ResponseEntity<Map<String, Object>> resp = handler.handleClientError(
                new HttpMessageNotReadableException("json inválido", (HttpInputMessage) null), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(appender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }
}
