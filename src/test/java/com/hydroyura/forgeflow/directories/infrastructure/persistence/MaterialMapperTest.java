package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MaterialMapperTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant DELETED = Instant.parse("2026-03-01T00:00:00Z");

    private final MaterialMapper mapper = MaterialMapper.INSTANCE;

    @Test
    void activeEntityRoundTrip_preservesAllFields() {
        MaterialId id = MaterialId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        Material material = Material.create(
                id, "45", "Сталь 45", StandardType.DIN, "DIN EN 10088-2", "кг",
                organizationId, CREATED, UPDATED);

        MaterialEntity entity = mapper.toEntity(material);
        Material restored = mapper.toDomain(entity);

        assertThat(restored.getId()).isEqualTo(id);
        assertThat(restored.getNumber()).isEqualTo("45");
        assertThat(restored.getName()).isEqualTo("Сталь 45");
        assertThat(restored.getStandardType()).isEqualTo(StandardType.DIN);
        assertThat(restored.getStandard()).isEqualTo("DIN EN 10088-2");
        assertThat(restored.getUnit()).isEqualTo("кг");
        assertThat(restored.getOrganizationId()).isEqualTo(organizationId);
        assertThat(restored.getCreatedAt()).isEqualTo(CREATED);
        assertThat(restored.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void toEntity_preservesDeletedAtAfterMarkDeleted() {
        Material material = Material.create(
                MaterialId.generate(), "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                OrganizationId.generate(), CREATED, UPDATED);
        material.markDeleted();

        MaterialEntity entity = mapper.toEntity(material);
        Material restored = mapper.toDomain(entity);

        assertThat(entity.deletedAt).isEqualTo(material.getDeletedAt()).isNotNull();
        // Восстановление через create не переносит deletedAt (активная проекция).
        assertThat(restored.getDeletedAt()).isNull();
    }
}
