package com.hydroyura.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.api.StandartView;
import com.hydroyura.forgeflow.catalog.domain.standart.Standart;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StandartMapperTest {

    @Test
    void mapsStandartToView() {
        StandartId id = StandartId.generate();
        OrganizationId org = OrganizationId.generate();
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-01-02T00:00:00Z");
        Standart standart = Standart.create(id, "С-1", "Стандарт", org, created, updated);

        StandartView view = StandartMapper.INSTANCE.toView(standart);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.number()).isEqualTo("С-1");
        assertThat(view.name()).isEqualTo("Стандарт");
        assertThat(view.createdAt()).isEqualTo(created);
        assertThat(view.updatedAt()).isEqualTo(updated);
    }
}
