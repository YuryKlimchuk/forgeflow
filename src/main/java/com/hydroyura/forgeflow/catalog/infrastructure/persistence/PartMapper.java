package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.domain.part.Part;
import com.hydroyura.forgeflow.shared.api.OrganizationId;

public final class PartMapper {

    public PartEntity toEntity(Part part) {
        PartEntity entity = new PartEntity();
        entity.id = part.getId().value();
        entity.number = part.getNumber();
        entity.name = part.getName();
        entity.organizationId = part.getOrganizationId().value();
        entity.createdAt = part.getCreatedAt();
        entity.updatedAt = part.getUpdatedAt();
        entity.deletedAt = part.getDeletedAt();
        return entity;
    }

    public Part toDomain(PartEntity entity) {
        return Part.create(PartId.of(entity.id), entity.number, entity.name,
                OrganizationId.of(entity.organizationId), entity.createdAt, entity.updatedAt);
    }

    public static final PartMapper INSTANCE = new PartMapper();

    private PartMapper() {
    }
}
