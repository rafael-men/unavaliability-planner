package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.UnavailabilityAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnavailabilityAuditRepository extends JpaRepository<UnavailabilityAudit, Long> {

    List<UnavailabilityAudit> findByUnavailabilityIdOrderByCreatedAtAsc(Long unavailabilityId);
}
