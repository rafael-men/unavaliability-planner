package com.unavaliability.backend.controllers;

import com.unavaliability.backend.dto.AuthDtos.LoginRequest;
import com.unavaliability.backend.dto.AuthDtos.LoginResponse;
import com.unavaliability.backend.dto.AuthDtos.RegisterRequest;
import com.unavaliability.backend.dto.AuthDtos.UserSummary;
import com.unavaliability.backend.security.CurrentUserProvider;
import com.unavaliability.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUser;

    public AuthController(AuthService authService, CurrentUserProvider currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req, HttpServletRequest http) {
        authService.checkLoginRate(clientIp(http), System.currentTimeMillis());
        return authService.login(req);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return Map.of("success", true,
                "message", "Cadastro realizado! Aguarde aprovação de um administrador.");
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        UserSummary user = authService.me(currentUser.require());
        return Map.of("user", user);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of("success", true);
    }

    private String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("x-forwarded-for");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        String real = http.getHeader("x-real-ip");
        return real != null && !real.isBlank() ? real : http.getRemoteAddr();
    }
}
