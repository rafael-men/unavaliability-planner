package com.unavaliability.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Migrations — nenhuma credencial")
class V2SeedRemovidoTest {

    @Test
    @DisplayName("V2 não insere usuário nem hash de senha")
    void v2NaoContemCredenciais() throws IOException {
        String sql;
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V2__seed_admin_master.sql")) {
            assertThat(in).isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        }

        assertThat(sql)
                .doesNotContain("insert into users5")
                .doesNotContain("$2a$")
                .doesNotContain("rafael@gmail.com");
    }
}
