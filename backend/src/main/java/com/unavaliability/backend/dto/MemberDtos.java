package com.unavaliability.backend.dto;


public final class MemberDtos {

    private MemberDtos() {
    }

    public record MemberRequest(
            String name,
            String email,
            String area,
            String squad,
            String funcao,
            String report_to,
            Boolean operacoes,
            Integer day_offs_quota) {
    }
}
