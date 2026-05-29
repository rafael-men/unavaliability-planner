package com.unavaliability.backend.dto;

import com.unavaliability.backend.models.Evento;

import java.time.LocalDate;
import java.util.List;


public final class EventoDtos {

    private EventoDtos() {
    }

    public record EventoRequest(
            String nome,
            String descricao,
            LocalDate data_inicio,
            LocalDate data_fim,
            List<Long> cliente_ids) {
    }

    public record ClienteRef(Long id, String nome) {
    }


    public record EventoView(
            Long id,
            String nome,
            String descricao,
            LocalDate data_inicio,
            LocalDate data_fim,
            List<Long> cliente_ids,
            List<ClienteRef> clientes) {

        public static EventoView of(Evento e, List<Long> clienteIds, List<ClienteRef> clientes) {
            return new EventoView(e.getId(), e.getNome(), e.getDescricao(),
                    e.getDataInicio(), e.getDataFim(), clienteIds, clientes);
        }
    }
}
