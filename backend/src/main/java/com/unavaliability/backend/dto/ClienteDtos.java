package com.unavaliability.backend.dto;


public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record ClienteRequest(String nome, String descricao, Boolean ativo) {
    }

    public record AssignUserRequest(Long user_id, Boolean ativo) {
    }

    public record UserClienteLink(Long user_id, Long cliente_id, Boolean ativo) {
    }
}
