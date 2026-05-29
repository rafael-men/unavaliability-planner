package com.unavaliability.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;


@Embeddable
public class EventoClienteId implements Serializable {

    @Column(name = "evento_id")
    private Long eventoId;
    @Column(name = "cliente_id")
    private Long clienteId;

    public EventoClienteId() {
    }

    public EventoClienteId(Long eventoId, Long clienteId) {
        this.eventoId = eventoId;
        this.clienteId = clienteId;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventoClienteId that = (EventoClienteId) o;
        return Objects.equals(eventoId, that.eventoId) && Objects.equals(clienteId, that.clienteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventoId, clienteId);
    }
}
