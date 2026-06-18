package com.unavaliability.backend.security;

import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.User;
import org.springframework.stereotype.Component;


@Component
public class AuthorizationService {

    public void requireAdmin(User actor) {
        if (!Roles.isAdmin(actor.getRole())) {
            throw ApiException.forbidden("Acesso restrito a administradores.");
        }
    }

    public void requireAdminEditor(User actor) {
        if (!Roles.isAdminEditor(actor.getRole())) {
            throw ApiException.forbidden("Apenas Admin Editor pode realizar esta ação.");
        }
    }

    public void requireMasterAdmin(User actor) {
        if (!Roles.isMasterAdmin(actor.getRole())) {
            throw ApiException.forbidden("Acesso exclusivo do Admin Master.");
        }
    }

    public void requireCanViewAll(User actor) {
        if (!Roles.canViewAll(actor.getRole())) {
            throw ApiException.forbidden("Acesso restrito a administradores.");
        }
    }
}
