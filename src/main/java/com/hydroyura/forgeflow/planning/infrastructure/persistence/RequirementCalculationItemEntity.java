package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "requirement_calculation_item")
public class RequirementCalculationItemEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "calculation_id")
    public UUID calculationId;

    @Column(name = "item_type")
    public String itemType;

    @Column(name = "item_id")
    public UUID itemId;

    @Column(name = "number")
    public String number;

    @Column(name = "name")
    public String name;

    @Column(name = "quantity")
    public int quantity;

    @Column(name = "need_type")
    public String needType;

    @Column(name = "unit")
    public String unit;
}
