package com.unavaliability.backend.repositories;

import com.unavaliability.backend.models.Unavailability;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface UnavailabilityRepository extends JpaRepository<Unavailability, Long> {

    interface AnalyticsSectorProjection {
        String getDepartment();
        Long getTotalDays();
        Long getSolicitacoes();
    }
    interface AnalyticsMonthProjection {
        String getMes();
        Long getTotalDays();
        Long getSolicitacoes();
    }
    interface AnalyticsApprovalProjection {
        Long getTotal();
        Long getAprovadas();
        Long getRejeitadas();
        Long getPendentes();
    }
    interface AnalyticsAverageApprovalProjection {
        BigDecimal getHorasMedia();
        Long getAmostras();
    }
    interface AnalyticsConflictProjection {
    Long getClienteId();
    String getClienteNome();
    Long getConflitos();
   }


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


//query params para carregamento de analytics
    @Query(value = """
        SELECT
            COALESCE(u.department, '(sem setor)') AS department,
            COALESCE(SUM(u.total_days), 0) AS totalDays,
            COUNT(*) AS solicitacoes
        FROM unavailability u
        WHERE u.status = 'approved'
          AND u.start_date <= :ate
          AND u.end_date >= :de
        GROUP BY COALESCE(u.department, '(sem setor)')
        ORDER BY COALESCE(SUM(u.total_days), 0) DESC
        """, nativeQuery = true)
    List<AnalyticsSectorProjection> findAnalyticsDaysByDepartment(
        @Param("de") LocalDate de,
        @Param("ate") LocalDate ate);

    @Query(value = """
        SELECT
            to_char(u.start_date, 'YYYY-MM') AS mes,
            COALESCE(SUM(u.total_days), 0) AS totalDays,
            COUNT(*) AS solicitacoes
        FROM unavailability u
        WHERE u.status = 'approved'
          AND u.start_date <= :ate
          AND u.end_date >= :de
        GROUP BY to_char(u.start_date, 'YYYY-MM')
        ORDER BY to_char(u.start_date, 'YYYY-MM')
        """, nativeQuery = true)
    List<AnalyticsMonthProjection> findAnalyticsDaysByMonth(
        @Param("de") LocalDate de,
        @Param("ate") LocalDate ate);

    @Query(value = """
        SELECT
            COUNT(*) AS total,
            COUNT(*) FILTER (WHERE u.status = 'approved') AS aprovadas,
            COUNT(*) FILTER (WHERE u.status = 'rejected') AS rejeitadas,
            COUNT(*) FILTER (WHERE u.status = 'pending') AS pendentes
        FROM unavailability u
        WHERE u.created_at >= :de
          AND u.created_at < :ate
        """, nativeQuery = true)
    AnalyticsApprovalProjection findAnalyticsApprovalRate(
        @Param("de") OffsetDateTime de,
        @Param("ate") OffsetDateTime ate);

    @Query(value = """
        SELECT
            COALESCE(
                ROUND(
                    (
                        AVG(
                            EXTRACT(
                                EPOCH FROM (u.reviewed_at - u.created_at)
                            )
                        ) / 3600.0
                    )::numeric,
                    2
                ),
                0
            ) AS horasMedia,
            COUNT(*) AS amostras
        FROM unavailability u
        WHERE u.created_at >= :de
          AND u.created_at < :ate
          AND u.reviewed_at IS NOT NULL
          AND u.created_at IS NOT NULL
          AND u.reviewed_at >= u.created_at
        """, nativeQuery = true)
    AnalyticsAverageApprovalProjection findAnalyticsAverageApprovalTime(
        @Param("de") OffsetDateTime de,
        @Param("ate") OffsetDateTime ate);

    @Query(value = """
        SELECT
            ec.cliente_id AS clienteId,
            c.nome AS clienteNome,
            COUNT(*) AS conflitos
        FROM unavailability u
        JOIN user_clientes uc
          ON uc.user_id = u.user_id
         AND uc.ativo = true
        JOIN evento_clientes ec
          ON ec.cliente_id = uc.cliente_id
        JOIN eventos e
          ON e.id = ec.evento_id
         AND e.data_inicio <= u.end_date
         AND e.data_fim >= u.start_date
        JOIN clientes c
          ON c.id = ec.cliente_id
        WHERE u.status = 'approved'
          AND u.start_date <= :ate
          AND u.end_date >= :de
          AND e.data_inicio <= :ate
          AND e.data_fim >= :de
        GROUP BY ec.cliente_id, c.nome
        ORDER BY COUNT(*) DESC
        """, nativeQuery = true)
    List<AnalyticsConflictProjection> findAnalyticsConflictRanking(
        @Param("de") LocalDate de,
        @Param("ate") LocalDate ate);

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
