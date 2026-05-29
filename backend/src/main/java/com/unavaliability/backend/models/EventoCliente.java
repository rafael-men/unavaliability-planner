package com.unavaliability.backend.models;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;



@Entity
@Table(name = "evento_clientes")
public class EventoCliente {

    @EmbeddedId
    private EventoClienteId id;

    public EventoCliente() {
    }

    public EventoCliente(EventoClienteId id) {
        this.id = id;
    }

    public EventoClienteId getId() {
        return id;
    }

    public void setId(EventoClienteId id) {
        this.id = id;
    }
}
