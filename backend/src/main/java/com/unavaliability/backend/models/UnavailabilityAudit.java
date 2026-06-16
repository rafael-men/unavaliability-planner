package com.unavaliability.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;


@Entity
@Table(name = "unavailability_audit")
public class UnavailabilityAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unavailability_id", nullable = false)
    private Long unavailabilityId;

    @Column(nullable = false)
    private String action;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name")
    private String actorName;

    @Column
    private String detail;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public UnavailabilityAudit() {
    }

    public UnavailabilityAudit(Long unavailabilityId, String action, Long actorId, String actorName, String detail) {
        this.unavailabilityId = unavailabilityId;
        this.action = action;
        this.actorId = actorId;
        this.actorName = actorName;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public Long getUnavailabilityId() {
        return unavailabilityId;
    }

    public String getAction() {
        return action;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getDetail() {
        return detail;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UnavailabilityAudit that = (UnavailabilityAudit) o;
        return Objects.equals(id, that.id) && Objects.equals(unavailabilityId, that.unavailabilityId) && Objects.equals(action, that.action) && Objects.equals(actorId, that.actorId) && Objects.equals(actorName, that.actorName) && Objects.equals(detail, that.detail) && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, unavailabilityId, action, actorId, actorName, detail, createdAt);
    }
}
