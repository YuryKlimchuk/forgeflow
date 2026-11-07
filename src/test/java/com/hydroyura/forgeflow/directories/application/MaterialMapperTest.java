package com.hydroyura.forgeflow.directories.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.MaterialView;
import com.hydroyura.forgeflow.directories.api.StandardType;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MaterialMapperTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");

    private final MaterialMapper mapper = new MaterialMapper();

    @Test
    void toView_mapsAllReadableFieldsAndOmitsOrganizationAndDeletedAt() {
        MaterialId id = MaterialId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        Material material = Material.create(
                id, "45", "Сталь 45", StandardType.GOST, "ГОСТ 1050-2013", "кг",
                organizationId, CREATED, UPDATED);

        MaterialView view = mapper.toView(material);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.number()).isEqualTo("45");
        assertThat(view.name()).isEqualTo("Сталь 45");
        assertThat(view.standardType()).isEqualTo(StandardType.GOST);
        assertThat(view.standard()).isEqualTo("ГОСТ 1050-2013");
        assertThat(view.unit()).isEqualTo("кг");
        assertThat(view.createdAt()).isEqualTo(CREATED);
        assertThat(view.updatedAt()).isEqualTo(UPDATED);
        assertThat(view.getClass().getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("organizationId", "deletedAt");
    }
}
