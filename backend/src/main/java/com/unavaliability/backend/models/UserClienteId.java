package com.unavaliability.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;


@Embeddable
public class UserClienteId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "cliente_id")
    private Long clienteId;

    public UserClienteId() {
    }

    public UserClienteId(Long userId, Long clienteId) {
        this.userId = userId;
        this.clienteId = clienteId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
        UserClienteId that = (UserClienteId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(clienteId, that.clienteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, clienteId);
    }
}
