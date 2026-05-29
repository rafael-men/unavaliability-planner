package com.unavaliability.backend.security;

import java.util.Set;


public final class Roles {

    public static final String ADMIN_MASTER = "admin_master";
    public static final String ADMIN_EDITOR = "admin_editor";
    public static final String ADMIN_LEITOR = "admin_leitor";
    public static final String LIDER = "lider";
    public static final String SOCIO = "socio";
    public static final String COLABORADOR = "colaborador";

    public static final String MASTER_ADMIN_EMAIL = "gustavo.romao@macfor.com.br";

    public static final Set<String> ASSIGNABLE_ROLES =
            Set.of(ADMIN_EDITOR, ADMIN_LEITOR, SOCIO, COLABORADOR, LIDER);

    private Roles() {
    }

    public static boolean isMasterAdmin(String role) {
        return ADMIN_MASTER.equals(role);
    }

    public static boolean isAdmin(String role) {
        return ADMIN_MASTER.equals(role) || ADMIN_EDITOR.equals(role) || ADMIN_LEITOR.equals(role);
    }

    public static boolean isAdminEditor(String role) {
        return ADMIN_MASTER.equals(role) || ADMIN_EDITOR.equals(role);
    }

    public static boolean isLider(String role) {
        return LIDER.equals(role);
    }

    public static boolean canViewAll(String role) {
        return isAdmin(role) || SOCIO.equals(role);
    }
}
