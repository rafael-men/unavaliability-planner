package com.unavaliability.backend.dto;

import java.util.List;


public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }


    public record DiasPorSetor(String department, long totalDays, long solicitacoes) {
    }
    public record DiasPorMes(String mes, long totalDays, long solicitacoes) {
    }
    public record TaxaAprovacao(
            long total, long aprovadas, long rejeitadas, long pendentes, double taxaAprovacaoPct) {
    }
    public record TempoMedioAprovacao(double horasMedia, long amostras) {
    }
    public record RankingConflito(Long clienteId, String clienteNome, long conflitos) {
    }

    public record DashboardAnalitico(
            List<DiasPorSetor> dias_por_setor,
            List<DiasPorMes> dias_por_mes,
            TaxaAprovacao taxa_aprovacao,
            TempoMedioAprovacao tempo_medio_aprovacao,
            List<RankingConflito> ranking_conflitos) {
    }
    public record SemanaForecast(
            String semanaInicio, String semanaFim, long pessoasFora, List<String> nomes) {
    }

    public record ForecastResponse(List<SemanaForecast> semanas) {
    }
}
