package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByOrderByNomeAsc();
    List<Cliente> findByAtivoTrueOrderByNomeAsc();
    Optional<Cliente> findByNomeIgnoreCase(String nome);
}
