package com.hydroyura.forgeflow.catalog.domain.assembly;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

@Entity
public class Assembly {

    @Identity
    private final AssemblyId id;

    private final String number;
    private final String name;
    private final OrganizationId organizationId;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private Assembly(AssemblyId id, String number, String name, OrganizationId organizationId, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.number = requireNotBlank(number, "number");
        this.name = requireNotBlank(name, "name");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Assembly create(AssemblyId id, String number, String name, OrganizationId organizationId, Instant createdAt, Instant updatedAt) {
        return new Assembly(id, number, name, organizationId, createdAt, updatedAt);
    }

    public void markDeleted() {
        if (deletedAt != null) {
            return;
        }
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
    }

    private static String requireNotBlank(String value, String field) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public AssemblyId getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
