package com.unavaliability.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Roles — hierarquia de papéis")
class RolesTest {

    @Test
    @DisplayName("isMasterAdmin só para admin_master")
    void masterAdmin() {
        assertThat(Roles.isMasterAdmin(Roles.ADMIN_MASTER)).isTrue();
        assertThat(Roles.isMasterAdmin(Roles.ADMIN_EDITOR)).isFalse();
        assertThat(Roles.isMasterAdmin(Roles.COLABORADOR)).isFalse();
    }

    @Test
    @DisplayName("isAdmin abrange master, editor e leitor")
    void admin() {
        assertThat(Roles.isAdmin(Roles.ADMIN_MASTER)).isTrue();
        assertThat(Roles.isAdmin(Roles.ADMIN_EDITOR)).isTrue();
        assertThat(Roles.isAdmin(Roles.ADMIN_LEITOR)).isTrue();
        assertThat(Roles.isAdmin(Roles.SOCIO)).isFalse();
        assertThat(Roles.isAdmin(Roles.LIDER)).isFalse();
    }

    @Test
    @DisplayName("isAdminEditor abrange master e editor, mas não leitor")
    void adminEditor() {
        assertThat(Roles.isAdminEditor(Roles.ADMIN_MASTER)).isTrue();
        assertThat(Roles.isAdminEditor(Roles.ADMIN_EDITOR)).isTrue();
        assertThat(Roles.isAdminEditor(Roles.ADMIN_LEITOR)).isFalse();
    }

    @Test
    @DisplayName("canViewAll abrange admins e sócio")
    void canViewAll() {
        assertThat(Roles.canViewAll(Roles.ADMIN_LEITOR)).isTrue();
        assertThat(Roles.canViewAll(Roles.SOCIO)).isTrue();
        assertThat(Roles.canViewAll(Roles.LIDER)).isFalse();
        assertThat(Roles.canViewAll(Roles.COLABORADOR)).isFalse();
    }

    @Test
    @DisplayName("admin_master NÃO é atribuível via API")
    void assignableRoles() {
        assertThat(Roles.ASSIGNABLE_ROLES).contains(
                Roles.ADMIN_EDITOR, Roles.ADMIN_LEITOR, Roles.SOCIO, Roles.LIDER, Roles.COLABORADOR);
        assertThat(Roles.ASSIGNABLE_ROLES).doesNotContain(Roles.ADMIN_MASTER);
    }

    @Test
    @DisplayName("papel nulo/desconhecido não tem privilégio")
    void papelInvalido() {
        assertThat(Roles.isAdmin(null)).isFalse();
        assertThat(Roles.isMasterAdmin("xpto")).isFalse();
        assertThat(Roles.canViewAll(null)).isFalse();
    }
}
