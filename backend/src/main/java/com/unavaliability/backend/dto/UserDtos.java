package com.unavaliability.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;


public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @Email @Size(max = 254) String email,
            @Size(max = 200) String password,
            @Size(max = 200) String full_name,
            @Size(max = 100) String department,
            @Size(max = 30) String role) {
    }

    public record ChangeRoleRequest(@Size(max = 30) String role) {
    }

    public record AssignSetorRequest(@Size(max = 100) String setor, Boolean is_lider) {
    }
}
