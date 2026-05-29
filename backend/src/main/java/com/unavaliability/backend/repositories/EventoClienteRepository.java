package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.EventoCliente;
import com.unavaliability.backend.models.EventoClienteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EventoClienteRepository extends JpaRepository<EventoCliente, EventoClienteId> {

    List<EventoCliente> findByIdEventoId(Long eventoId);
    List<EventoCliente> findByIdEventoIdIn(List<Long> eventoIds);
    List<EventoCliente> findByIdClienteId(Long clienteId);
    @Transactional
    void deleteByIdEventoId(Long eventoId);
}
