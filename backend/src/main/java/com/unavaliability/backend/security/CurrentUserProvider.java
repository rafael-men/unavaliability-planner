package com.unavaliability.backend.security;

import com.unavaliability.backend.exception.ApiException;
import com.unavaliability.backend.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
public class CurrentUserProvider {

    public User require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw ApiException.unauthorized("Não autenticado. Faça login.");
        }
        return principal.getUser();
    }
}
