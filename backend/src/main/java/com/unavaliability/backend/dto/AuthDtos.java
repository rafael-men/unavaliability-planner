package com.unavaliability.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @Email @Size(max = 254) String email,
            @Size(max = 200) String password) {
    }

    public record RegisterRequest(
            @Email @Size(max = 254) String email,
            @Size(max = 200) String password,
            @Size(max = 200) String full_name,
            @Size(max = 100) String department) {
    }

    public record UserSummary(Long id, String email, String full_name, String role) {
    }

    public record LoginResponse(boolean success, String token, UserSummary user) {
    }
}
