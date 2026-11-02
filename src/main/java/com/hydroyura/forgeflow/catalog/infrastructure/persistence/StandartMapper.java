package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.domain.standart.Standart;
import com.hydroyura.forgeflow.shared.api.OrganizationId;

public final class StandartMapper {

    public StandartEntity toEntity(Standart standart) {
        StandartEntity entity = new StandartEntity();
        entity.id = standart.getId().value();
        entity.number = standart.getNumber();
        entity.name = standart.getName();
        entity.organizationId = standart.getOrganizationId().value();
        entity.createdAt = standart.getCreatedAt();
        entity.updatedAt = standart.getUpdatedAt();
        entity.deletedAt = standart.getDeletedAt();
        return entity;
    }

    public Standart toDomain(StandartEntity entity) {
        return Standart.create(StandartId.of(entity.id), entity.number, entity.name,
                OrganizationId.of(entity.organizationId), entity.createdAt, entity.updatedAt);
    }

    public static final StandartMapper INSTANCE = new StandartMapper();

    private StandartMapper() {
    }
}
