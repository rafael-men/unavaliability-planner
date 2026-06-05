package com.unavaliability.backend.dto;

import com.unavaliability.backend.models.Unavailability;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;


public final class UnavailabilityDtos {

    private UnavailabilityDtos() {
    }

    public record CreateRequest(
            @Size(max = 30) String unavailability_type,
            @Size(max = 100) String department,
            LocalDate start_date,
            LocalDate end_date,
            Integer total_days) {
    }

    public record UpdateRequest(
            LocalDate start_date,
            LocalDate end_date,
            @Size(max = 30) String unavailability_type,
            @Size(max = 100) String department) {
    }

    public record EventConflict(
            Long id, String nome, String descricao,
            LocalDate data_inicio, LocalDate data_fim, List<String> clientes) {
    }


    public record UnavailabilityView(
            Long id,
            Long user_id,
            String full_name,
            String user_name,
            String user_email,
            String user_role,
            String unavailability_type,
            String department,
            LocalDate start_date,
            LocalDate end_date,
            Integer total_days,
            String status,
            Long reviewed_by,
            OffsetDateTime reviewed_at,
            OffsetDateTime created_at,
            List<EventConflict> event_conflicts) {
    }

    public record ListResponse(List<UnavailabilityView> data, boolean truncated) {
    }

    public record UsersOnLeave(
            Long user_id, String user_name, String department,
            LocalDate start_date, LocalDate end_date, Integer total_days) {
    }

    public record DepartmentImpact(String department, int on_leave, List<String> on_leave_names) {
    }

    public record ImpactResponse(List<UsersOnLeave> users_on_leave, List<DepartmentImpact> department_impact) {
    }

    public static UnavailabilityView toView(Unavailability u) {
        return new UnavailabilityView(
                u.getId(), u.getUserId(), u.getFullName(), null, null, null,
                u.getUnavailabilityType(), u.getDepartment(), u.getStartDate(), u.getEndDate(),
                u.getTotalDays(), u.getStatus(), u.getReviewedBy(), u.getReviewedAt(),
                u.getCreatedAt(), null);
    }
}
