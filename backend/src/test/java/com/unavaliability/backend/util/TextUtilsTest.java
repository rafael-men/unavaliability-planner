package com.unavaliability.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextUtils — saneamento e validação")
class TextUtilsTest {

    @Test
    @DisplayName("cleanText faz trim e trata null como string vazia")
    void cleanTextTrimENull() {
        assertThat(TextUtils.cleanText("  abc  ")).isEqualTo("abc");
        assertThat(TextUtils.cleanText(null)).isEmpty();
    }

    @Test
    @DisplayName("cleanText corta no tamanho máximo")
    void cleanTextCorte() {
        String longo = "x".repeat(600);
        assertThat(TextUtils.cleanText(longo)).hasSize(500);
        assertThat(TextUtils.cleanText(longo, 10)).hasSize(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@b.com", "rafael@gmail.com", "x.y@dominio.com.br"})
    @DisplayName("emails válidos passam")
    void emailValido(String email) {
        assertThat(TextUtils.isValidEmail(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"semarroba", "a@", "@b.com", "a b@c.com", "a@b"})
    @DisplayName("emails inválidos falham")
    void emailInvalido(String email) {
        assertThat(TextUtils.isValidEmail(email)).isFalse();
    }

    @Test
    @DisplayName("isValidEmail trata null como inválido")
    void emailNull() {
        assertThat(TextUtils.isValidEmail(null)).isFalse();
    }

    @Test
    @DisplayName("toBool coage Boolean, número e string")
    void toBool() {
        assertThat(TextUtils.toBool(Boolean.TRUE)).isTrue();
        assertThat(TextUtils.toBool(1)).isTrue();
        assertThat(TextUtils.toBool("true")).isTrue();
        assertThat(TextUtils.toBool("1")).isTrue();
        assertThat(TextUtils.toBool(0)).isFalse();
        assertThat(TextUtils.toBool("false")).isFalse();
        assertThat(TextUtils.toBool(null)).isFalse();
    }
}
