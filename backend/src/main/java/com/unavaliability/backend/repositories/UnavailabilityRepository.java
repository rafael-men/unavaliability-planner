package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.Unavailability;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UnavailabilityRepository extends JpaRepository<Unavailability, Long> {


    @Query(value = """
        SELECT COUNT(DISTINCT ua.id)
        FROM unavailability ua
        JOIN users5 requester ON requester.id = ua.user_id
        LEFT JOIN members member
            ON member.id = requester.member_id
            OR (
                requester.member_id IS NULL
                AND lower(member.email) = lower(requester.email)
            )
        WHERE ua.status = :status
          AND requester.id <> :leaderId
          AND (
                (
                    :department IS NOT NULL
                    AND requester.department = :department
                )
                OR (
                    :leaderEmail IS NOT NULL
                    AND (
                        (
                            ',' || trim(
                                regexp_replace(
                                    lower(coalesce(member.report_to, '')),
                                    '\\s*[,;]\\s*',
                                    ',',
                                    'g'
                                )
                            ) || ','
                        ) LIKE '%,' || lower(trim(:leaderEmail)) || ',%'
                        OR
                        (
                            :leaderName IS NOT NULL
                            AND (
                                ',' || trim(
                                    regexp_replace(
                                        lower(coalesce(member.report_to, '')),
                                        '\\s*[,;]\\s*',
                                        ',',
                                        'g'
                                    )
                                ) || ','
                            ) LIKE '%,' || lower(trim(:leaderName)) || ',%'
                        )
                    )
                )
          )
        """, nativeQuery = true)
    long countPendingVisibleToLeader(
        @Param("status") String status,
        @Param("leaderId") Long leaderId,
        @Param("department") String department,
        @Param("leaderEmail") String leaderEmail,
        @Param("leaderName") String leaderName);
    List<Unavailability> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Unavailability> findByStatusOrderByCreatedAtDesc(String status);
    List<Unavailability> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(String status);
    List<Unavailability> findByUserIdAndStatusIn(Long userId, List<String> statuses);
    List<Unavailability> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
            String status, LocalDate startDate, LocalDate endDate);
    List<Unavailability> findByUserIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, String status, LocalDate yearEnd, LocalDate yearStart);
    List<Unavailability> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String status, LocalDate ate, LocalDate de);
    List<Unavailability> findByCreatedAtBetween(
            java.time.OffsetDateTime de, java.time.OffsetDateTime ate);
}
