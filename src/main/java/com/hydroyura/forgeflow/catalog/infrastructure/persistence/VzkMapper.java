package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.domain.vzk.Vzk;
import com.hydroyura.forgeflow.shared.api.OrganizationId;

public final class VzkMapper {

    public VzkEntity toEntity(Vzk vzk) {
        VzkEntity entity = new VzkEntity();
        entity.id = vzk.getId().value();
        entity.number = vzk.getNumber();
        entity.name = vzk.getName();
        entity.organizationId = vzk.getOrganizationId().value();
        entity.createdAt = vzk.getCreatedAt();
        entity.updatedAt = vzk.getUpdatedAt();
        entity.deletedAt = vzk.getDeletedAt();
        return entity;
    }

    public Vzk toDomain(VzkEntity entity) {
        return Vzk.create(VzkId.of(entity.id), entity.number, entity.name,
                OrganizationId.of(entity.organizationId), entity.createdAt, entity.updatedAt);
    }

    public static final VzkMapper INSTANCE = new VzkMapper();

    private VzkMapper() {
    }
}
