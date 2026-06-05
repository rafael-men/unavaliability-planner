package com.unavaliability.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTOs de administração de membros. Limites de tamanho como defesa contra DoS.
 */
public final class MemberDtos {

    private MemberDtos() {
    }

    public record MemberRequest(
            @Size(max = 200) String name,
            @Email @Size(max = 254) String email,
            @Size(max = 100) String area,
            @Size(max = 100) String squad,
            @Size(max = 100) String funcao,
            @Size(max = 1000) String report_to,
            Boolean operacoes,
            Integer day_offs_quota) {
    }
}
