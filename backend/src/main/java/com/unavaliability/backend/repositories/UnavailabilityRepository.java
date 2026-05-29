package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.Unavailability;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UnavailabilityRepository extends JpaRepository<Unavailability, Long> {

    List<Unavailability> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Unavailability> findByStatusOrderByCreatedAtDesc(String status);
    List<Unavailability> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Unavailability> findByUserIdAndStatusIn(Long userId, List<String> statuses);
    List<Unavailability> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
            String status, LocalDate startDate, LocalDate endDate);
    List<Unavailability> findByUserIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, String status, LocalDate yearEnd, LocalDate yearStart);
}
