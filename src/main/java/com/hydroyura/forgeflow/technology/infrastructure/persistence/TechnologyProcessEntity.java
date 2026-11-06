package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "technology_process")
public class TechnologyProcessEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "number")
    public String number;

    @Column(name = "name")
    public String name;

    @Column(name = "part_id")
    public UUID partId;

    @Column(name = "organization_id")
    public UUID organizationId;

    @Column(name = "default_process")
    public boolean defaultProcess;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public Instant deletedAt;
}
