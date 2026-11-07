package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.shared.api.OrganizationId;

/**
 * Инфраструктурный маппер entity {@literal <->} domain. Восстановление агрегата
 * выполняется через {@link Material#create} без {@code deletedAt}: чтение
 * фильтрует только активные записи, а {@code deletedAt} пишется только в entity.
 */
public final class MaterialMapper {

    public MaterialEntity toEntity(Material material) {
        MaterialEntity entity = new MaterialEntity();
        entity.id = material.getId().value();
        entity.number = material.getNumber();
        entity.name = material.getName();
        entity.standardType = material.getStandardType();
        entity.standard = material.getStandard();
        entity.unit = material.getUnit();
        entity.organizationId = material.getOrganizationId().value();
        entity.createdAt = material.getCreatedAt();
        entity.updatedAt = material.getUpdatedAt();
        entity.deletedAt = material.getDeletedAt();
        return entity;
    }

    public Material toDomain(MaterialEntity entity) {
        return Material.create(
                MaterialId.of(entity.id),
                entity.number,
                entity.name,
                entity.standardType,
                entity.standard,
                entity.unit,
                OrganizationId.of(entity.organizationId),
                entity.createdAt,
                entity.updatedAt);
    }

    public static final MaterialMapper INSTANCE = new MaterialMapper();

    private MaterialMapper() {
    }
}
