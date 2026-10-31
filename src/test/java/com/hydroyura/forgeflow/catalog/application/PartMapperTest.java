package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.PartView;
import com.hydroyura.forgeflow.catalog.domain.part.Part;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PartMapperTest {

    @Test
    void mapsPartToView() {
        PartId id = PartId.generate();
        OrganizationId org = OrganizationId.generate();
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-01-02T00:00:00Z");
        Part part = Part.create(id, "P-1", "Деталь", org, created, updated);

        PartView view = PartMapper.INSTANCE.toView(part);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.number()).isEqualTo("P-1");
        assertThat(view.name()).isEqualTo("Деталь");
        assertThat(view.createdAt()).isEqualTo(created);
        assertThat(view.updatedAt()).isEqualTo(updated);
    }
}
