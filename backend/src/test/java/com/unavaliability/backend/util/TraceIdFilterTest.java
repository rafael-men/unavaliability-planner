package com.unavaliability.backend.util;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TraceIdFilter — correlação de requisições")
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void limpaMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("gera um id quando o header não vem, expõe no MDC e na resposta, e limpa ao final")
    void geraIdQuandoAusente() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<String> dentroDaCadeia = new AtomicReference<>();
        FilterChain chain = (r, s) -> dentroDaCadeia.set(MDC.get(TraceIdFilter.MDC_KEY));

        filter.doFilter(req, res, chain);

        assertThat(dentroDaCadeia.get()).isNotBlank();
        assertThat(res.getHeader(TraceIdFilter.HEADER)).isEqualTo(dentroDaCadeia.get());
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("reaproveita o X-Trace-Id enviado pelo cliente quando o valor é seguro")
    void reaproveitaIdValido() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/health");
        req.addHeader(TraceIdFilter.HEADER, "abc-123-XYZ-valido");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (r, s) -> { });

        assertThat(res.getHeader(TraceIdFilter.HEADER)).isEqualTo("abc-123-XYZ-valido");
    }

    @Test
    @DisplayName("descarta valor com quebra de linha ou caracteres fora do padrão (anti log injection)")
    void descartaIdMalicioso() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/health");
        req.addHeader(TraceIdFilter.HEADER, "x\r\nERROR falso");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (r, s) -> { });

        String devolvido = res.getHeader(TraceIdFilter.HEADER);
        assertThat(devolvido).isNotNull().doesNotContain("\r", "\n", " ");
        assertThat(devolvido).isNotEqualTo("x\r\nERROR falso");
    }

    @Test
    @DisplayName("limpa o MDC mesmo quando a cadeia lança exceção")
    void limpaMdcComExcecao() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/boom");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> { throw new IllegalStateException("boom"); };

        assertThatThrownBy(() -> filter.doFilter(req, res, chain))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
 
