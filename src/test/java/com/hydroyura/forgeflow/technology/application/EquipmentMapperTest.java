package com.hydroyura.forgeflow.technology.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.EquipmentView;
import com.hydroyura.forgeflow.technology.domain.equipment.Equipment;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EquipmentMapperTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");

    private final EquipmentMapper mapper = new EquipmentMapper();

    @Test
    void toView_mapsAllReadableFieldsAndOmitsOrganizationAndDeletedAt() {
        EquipmentId id = EquipmentId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        Equipment equipment = Equipment.create(
                id, "С-001", "Станок токарный", "Токарный", organizationId, CREATED, UPDATED);

        EquipmentView view = mapper.toView(equipment);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.number()).isEqualTo("С-001");
        assertThat(view.name()).isEqualTo("Станок токарный");
        assertThat(view.type()).isEqualTo("Токарный");
        assertThat(view.createdAt()).isEqualTo(CREATED);
        assertThat(view.updatedAt()).isEqualTo(UPDATED);
        assertThat(view.getClass().getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("organizationId", "deletedAt");
    }
}
