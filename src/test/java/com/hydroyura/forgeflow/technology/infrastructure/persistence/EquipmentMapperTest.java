package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EquipmentMapperTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");

    private final EquipmentMapper mapper = EquipmentMapper.INSTANCE;

    @Test
    void activeEntityRoundTrip_preservesAllFields() {
        EquipmentId id = EquipmentId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        Equipment equipment = Equipment.create(
                id, "С-001", "Станок токарный", "Токарный", organizationId, CREATED, UPDATED);

        EquipmentEntity entity = mapper.toEntity(equipment);
        Equipment restored = mapper.toDomain(entity);

        assertThat(restored.getId()).isEqualTo(id);
        assertThat(restored.getNumber()).isEqualTo("С-001");
        assertThat(restored.getName()).isEqualTo("Станок токарный");
        assertThat(restored.getType()).isEqualTo("Токарный");
        assertThat(restored.getOrganizationId()).isEqualTo(organizationId);
        assertThat(restored.getCreatedAt()).isEqualTo(CREATED);
        assertThat(restored.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void toEntity_preservesDeletedAtAfterMarkDeleted() {
        Equipment equipment = Equipment.create(
                EquipmentId.generate(), "С-001", "Станок токарный", "Токарный",
                OrganizationId.generate(), CREATED, UPDATED);
        equipment.markDeleted();

        EquipmentEntity entity = mapper.toEntity(equipment);
        Equipment restored = mapper.toDomain(entity);

        assertThat(entity.deletedAt).isEqualTo(equipment.getDeletedAt()).isNotNull();
        // Восстановление через create не переносит deletedAt (активная проекция).
        assertThat(restored.getDeletedAt()).isNull();
    }
}
