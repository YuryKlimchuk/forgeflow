package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "substitution")
@IdClass(SubstitutionId.class)
public class SubstitutionEntity {

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

    @Id
    @Column(name = "substitute_type")
    public String substituteType;

    @Id
    @Column(name = "substitute_id")
    public UUID substituteId;
}
