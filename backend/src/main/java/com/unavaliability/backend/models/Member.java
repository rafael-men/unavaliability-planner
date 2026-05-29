package com.unavaliability.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.Objects;


@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Column(nullable = false)
    private String name;
    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;
    @Column
    private String area;
    @Column
    private String squad;
    @Column
    private String funcao;
    @Column(name = "report_to", columnDefinition = "text")
    private String reportTo;
    @Column
    private Boolean operacoes;
    @Column(name = "day_offs_quota")
    private Integer dayOffsQuota;
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Member() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getSquad() {
        return squad;
    }

    public void setSquad(String squad) {
        this.squad = squad;
    }

    public String getFuncao() {
        return funcao;
    }

    public void setFuncao(String funcao) {
        this.funcao = funcao;
    }

    public String getReportTo() {
        return reportTo;
    }

    public void setReportTo(String reportTo) {
        this.reportTo = reportTo;
    }

    public Boolean getOperacoes() {
        return operacoes;
    }

    public void setOperacoes(Boolean operacoes) {
        this.operacoes = operacoes;
    }

    public Integer getDayOffsQuota() {
        return dayOffsQuota;
    }

    public void setDayOffsQuota(Integer dayOffsQuota) {
        this.dayOffsQuota = dayOffsQuota;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return Objects.equals(id, member.id) && Objects.equals(name, member.name) && Objects.equals(email, member.email) && Objects.equals(area, member.area) && Objects.equals(squad, member.squad) && Objects.equals(funcao, member.funcao) && Objects.equals(reportTo, member.reportTo) && Objects.equals(operacoes, member.operacoes) && Objects.equals(dayOffsQuota, member.dayOffsQuota) && Objects.equals(createdAt, member.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, area, squad, funcao, reportTo, operacoes, dayOffsQuota, createdAt);
    }
}
