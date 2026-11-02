package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "specification_row")
@IdClass(SpecificationRowId.class)
public class SpecificationRowEntity {

    @Id
    @Column(name = "organization_id")
    public UUID organizationId;

    @Id
    @Column(name = "assembly_id")
    public UUID assemblyId;

    @Id
    @Column(name = "item_type")
    public String itemType;

    @Id
    @Column(name = "item_id")
    public UUID itemId;

    @Column(name = "quantity")
    public int quantity;
}
