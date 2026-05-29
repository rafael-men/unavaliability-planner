package com.unavaliability.backend.dto;


public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            String email, String password, String full_name, String department, String role) {
    }

    public record ChangeRoleRequest(String role) {
    }

    public record AssignSetorRequest(String setor, Boolean is_lider) {
    }
}
