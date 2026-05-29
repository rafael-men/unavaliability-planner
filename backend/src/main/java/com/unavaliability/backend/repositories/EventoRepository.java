package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findAllByOrderByDataInicioDesc();
    List<Evento> findByDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioAsc(
            LocalDate maxEnd, LocalDate minStart);
}
