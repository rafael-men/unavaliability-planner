package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.UserCliente;
import com.unavaliability.backend.models.UserClienteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserClienteRepository extends JpaRepository<UserCliente, UserClienteId> {

    List<UserCliente> findByIdUserIdAndAtivoTrue(Long userId);
    List<UserCliente> findByIdClienteIdAndAtivoTrue(Long clienteId);
    List<UserCliente> findByIdUserIdInAndAtivoTrue(List<Long> userIds);
}
