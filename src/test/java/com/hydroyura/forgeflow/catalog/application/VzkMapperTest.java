package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.api.VzkView;
import com.hydroyura.forgeflow.catalog.domain.vzk.Vzk;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VzkMapperTest {

    @Test
    void mapsVzkToView() {
        VzkId id = VzkId.generate();
        OrganizationId org = OrganizationId.generate();
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-01-02T00:00:00Z");
        Vzk vzk = Vzk.create(id, "В-1", "Группа", org, created, updated);

        VzkView view = VzkMapper.INSTANCE.toView(vzk);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.number()).isEqualTo("В-1");
        assertThat(view.name()).isEqualTo("Группа");
        assertThat(view.createdAt()).isEqualTo(created);
        assertThat(view.updatedAt()).isEqualTo(updated);
    }
}
