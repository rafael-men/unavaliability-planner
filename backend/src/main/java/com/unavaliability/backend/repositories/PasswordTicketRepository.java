package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.PasswordTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordTicketRepository extends JpaRepository<PasswordTicket, Long> {

    List<PasswordTicket> findByStatusOrderByCreatedAtDesc(String status);

    List<PasswordTicket> findAllByOrderByCreatedAtDesc();

    boolean existsByEmailIgnoreCaseAndStatus(String email, String status);
}
