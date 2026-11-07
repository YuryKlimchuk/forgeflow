package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.shared.api.OrganizationId;

/**
 * Инфраструктурный маппер entity {@literal <->} domain. Восстановление агрегата
 * выполняется через {@link Sortament#create} без {@code deletedAt}: чтение
 * фильтрует только активные записи, а {@code deletedAt} пишется только в entity.
 */
public final class SortamentMapper {

    public SortamentEntity toEntity(Sortament sortament) {
        SortamentEntity entity = new SortamentEntity();
        entity.id = sortament.getId().value();
        entity.number = sortament.getNumber();
        entity.name = sortament.getName();
        entity.standardType = sortament.getStandardType();
        entity.standard = sortament.getStandard();
        entity.unit = sortament.getUnit();
        entity.organizationId = sortament.getOrganizationId().value();
        entity.createdAt = sortament.getCreatedAt();
        entity.updatedAt = sortament.getUpdatedAt();
        entity.deletedAt = sortament.getDeletedAt();
        return entity;
    }

    public Sortament toDomain(SortamentEntity entity) {
        return Sortament.create(
                SortamentId.of(entity.id),
                entity.number,
                entity.name,
                entity.standardType,
                entity.standard,
                entity.unit,
                OrganizationId.of(entity.organizationId),
                entity.createdAt,
                entity.updatedAt);
    }

    public static final SortamentMapper INSTANCE = new SortamentMapper();

    private SortamentMapper() {
    }
}
