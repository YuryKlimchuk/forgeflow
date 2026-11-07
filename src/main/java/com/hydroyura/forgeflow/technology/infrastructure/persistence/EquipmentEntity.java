package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "equipment")
public class EquipmentEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "number")
    public String number;

    @Column(name = "name")
    public String name;

    @Column(name = "type")
    public String type;

    @Column(name = "organization_id")
    public UUID organizationId;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public Instant deletedAt;
}
