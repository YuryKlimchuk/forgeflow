package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;

/**
 * Общая JPA-модель пяти саб-доменов каталога. Идентификатор присваивается
 * доменом (без {@code @GeneratedValue}).
 */
@MappedSuperclass
public abstract class CatalogEntity {

    @Id
    public UUID id;

    public String number;

    public String name;

    public UUID organizationId;

    public Instant createdAt;

    public Instant updatedAt;

    public Instant deletedAt;
}
