package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "requirement_calculation_item_source")
@IdClass(RequirementCalculationItemSourceId.class)
public class RequirementCalculationItemSourceEntity {

    @Id
    @Column(name = "calculation_item_id")
    public UUID calculationItemId;

    @Id
    @Column(name = "source_assembly_id")
    public UUID sourceAssemblyId;
}
