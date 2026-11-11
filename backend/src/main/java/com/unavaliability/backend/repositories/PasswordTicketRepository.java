package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.PasswordTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordTicketRepository extends JpaRepository<PasswordTicket, Long> {

    List<PasswordTicket> findByStatusOrderByCreatedAtDesc(String status);

    List<PasswordTicket> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    boolean existsByEmailIgnoreCaseAndStatus(String email, String status);

    Optional<PasswordTicket> findByTokenHashAndStatus(String tokenHash, String status);
}
