package com.unavaliability.backend.dto;


public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(String email, String password) {
    }

    public record RegisterRequest(String email, String password, String full_name, String department) {
    }

    public record UserSummary(Long id, String email, String full_name, String role) {
    }

    public record LoginResponse(boolean success, String token, UserSummary user) {
    }
}
