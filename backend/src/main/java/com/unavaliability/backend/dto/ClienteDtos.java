package com.unavaliability.backend.dto;

import jakarta.validation.constraints.Size;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record ClienteRequest(
            @Size(max = 200) String nome,
            @Size(max = 2000) String descricao,
            Boolean ativo) {
    }

    public record AssignUserRequest(Long user_id, Boolean ativo) {
    }

    public record UserClienteLink(Long user_id, Long cliente_id, Boolean ativo) {
    }
}
