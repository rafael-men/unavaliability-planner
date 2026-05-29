package com.unavaliability.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;


@Entity
@Table(name = "user_clientes")
public class UserCliente {

    @EmbeddedId
    private UserClienteId id;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UserCliente() {
    }

    public UserCliente(UserClienteId id, Boolean ativo) {
        this.id = id;
        this.ativo = ativo;
    }

    public UserClienteId getId() {
        return id;
    }

    public void setId(UserClienteId id) {
        this.id = id;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
