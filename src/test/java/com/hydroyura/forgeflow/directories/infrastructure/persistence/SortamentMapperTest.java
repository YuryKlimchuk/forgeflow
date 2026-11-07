package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SortamentMapperTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");

    private final SortamentMapper mapper = SortamentMapper.INSTANCE;

    @Test
    void activeEntityRoundTrip_preservesAllFields() {
        SortamentId id = SortamentId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        Sortament sortament = Sortament.create(
                id, "Круг В-30", "Круг горячекатаный В-30", StandardType.DIN, "DIN EN 10210", "мм",
                organizationId, CREATED, UPDATED);

        SortamentEntity entity = mapper.toEntity(sortament);
        Sortament restored = mapper.toDomain(entity);

        assertThat(restored.getId()).isEqualTo(id);
        assertThat(restored.getNumber()).isEqualTo("Круг В-30");
        assertThat(restored.getName()).isEqualTo("Круг горячекатаный В-30");
        assertThat(restored.getStandardType()).isEqualTo(StandardType.DIN);
        assertThat(restored.getStandard()).isEqualTo("DIN EN 10210");
        assertThat(restored.getUnit()).isEqualTo("мм");
        assertThat(restored.getOrganizationId()).isEqualTo(organizationId);
        assertThat(restored.getCreatedAt()).isEqualTo(CREATED);
        assertThat(restored.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void toEntity_preservesDeletedAtAfterMarkDeleted() {
        Sortament sortament = Sortament.create(
                SortamentId.generate(), "Круг В-30", "Круг горячекатаный В-30",
                StandardType.GOST, "ГОСТ 2590-2006", "мм",
                OrganizationId.generate(), CREATED, UPDATED);
        sortament.markDeleted();

        SortamentEntity entity = mapper.toEntity(sortament);
        Sortament restored = mapper.toDomain(entity);

        assertThat(entity.deletedAt).isEqualTo(sortament.getDeletedAt()).isNotNull();
        assertThat(restored.getDeletedAt()).isNull();
    }
}
