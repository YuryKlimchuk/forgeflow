package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "plan_item")
@IdClass(PlanItemId.class)
public class PlanItemEntity {

    @Id
    @Column(name = "plan_id")
    public UUID planId;

    @Id
    @Column(name = "assembly_id")
    public UUID assemblyId;

    @Column(name = "quantity")
    public int quantity;
}
