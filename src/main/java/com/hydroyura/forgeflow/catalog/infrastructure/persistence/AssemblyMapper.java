package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.shared.api.OrganizationId;

public final class AssemblyMapper {

    public AssemblyEntity toEntity(Assembly assembly) {
        AssemblyEntity entity = new AssemblyEntity();
        entity.id = assembly.getId().value();
        entity.number = assembly.getNumber();
        entity.name = assembly.getName();
        entity.organizationId = assembly.getOrganizationId().value();
        entity.createdAt = assembly.getCreatedAt();
        entity.updatedAt = assembly.getUpdatedAt();
        entity.deletedAt = assembly.getDeletedAt();
        return entity;
    }

    public Assembly toDomain(AssemblyEntity entity) {
        return Assembly.create(AssemblyId.of(entity.id), entity.number, entity.name,
                OrganizationId.of(entity.organizationId), entity.createdAt, entity.updatedAt);
    }

    public static final AssemblyMapper INSTANCE = new AssemblyMapper();

    private AssemblyMapper() {
    }
}
