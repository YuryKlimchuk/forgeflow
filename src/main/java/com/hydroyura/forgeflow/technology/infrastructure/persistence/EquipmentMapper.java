package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;

/**
 * Инфраструктурный маппер entity {@literal <->} domain. Восстановление агрегата
 * выполняется через {@link Equipment#create} без {@code deletedAt}: чтение
 * фильтрует только активные записи, а {@code deletedAt} пишется только в entity.
 */
public final class EquipmentMapper {

    public EquipmentEntity toEntity(Equipment equipment) {
        EquipmentEntity entity = new EquipmentEntity();
        entity.id = equipment.getId().value();
        entity.number = equipment.getNumber();
        entity.name = equipment.getName();
        entity.type = equipment.getType();
        entity.organizationId = equipment.getOrganizationId().value();
        entity.createdAt = equipment.getCreatedAt();
        entity.updatedAt = equipment.getUpdatedAt();
        entity.deletedAt = equipment.getDeletedAt();
        return entity;
    }

    public Equipment toDomain(EquipmentEntity entity) {
        return Equipment.create(
                EquipmentId.of(entity.id),
                entity.number,
                entity.name,
                entity.type,
                OrganizationId.of(entity.organizationId),
                entity.createdAt,
                entity.updatedAt);
    }

    public static final EquipmentMapper INSTANCE = new EquipmentMapper();

    private EquipmentMapper() {
    }
}
