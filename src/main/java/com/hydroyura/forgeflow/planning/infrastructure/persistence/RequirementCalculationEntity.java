package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "requirement_calculation")
public class RequirementCalculationEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "organization_id")
    public UUID organizationId;

    @Column(name = "plan_id")
    public UUID planId;

    @Column(name = "executed_at")
    public Instant executedAt;
}
