package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "plan")
public class PlanEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "organization_id")
    public UUID organizationId;

    @Column(name = "name")
    public String name;

    @Column(name = "year")
    public int year;

    @Column(name = "month")
    public int month;
}
