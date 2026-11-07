package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import com.hydroyura.forgeflow.directories.api.StandardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "material")
public class MaterialEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "number")
    public String number;

    @Column(name = "name")
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "standard_type")
    public StandardType standardType;

    @Column(name = "standard")
    public String standard;

    @Column(name = "unit")
    public String unit;

    @Column(name = "organization_id")
    public UUID organizationId;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public Instant deletedAt;
}
