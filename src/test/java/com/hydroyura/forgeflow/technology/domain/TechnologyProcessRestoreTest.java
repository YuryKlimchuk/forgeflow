package com.hydroyura.forgeflow.technology.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TechnologyProcessRestoreTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant DELETED = Instant.parse("2026-03-01T00:00:00Z");

    @Test
    void restore_preservesAllFieldsAndSortsOperationsWithoutTouchingUpdatedAt() {
        TechnologyProcessId id = TechnologyProcessId.generate();
        PartId partId = PartId.generate();
        OrganizationId organizationId = OrganizationId.generate();

        TechnologyProcess process = TechnologyProcess.restore(
                id, "TP-1", "Фрезеровка", partId, organizationId, true,
                CREATED, UPDATED, DELETED,
                List.of(new Operation(20, "Б"), new Operation(10, "А")));

        assertThat(process.getId()).isEqualTo(id);
        assertThat(process.getNumber()).isEqualTo("TP-1");
        assertThat(process.getName()).isEqualTo("Фрезеровка");
        assertThat(process.getPartId()).isEqualTo(partId);
        assertThat(process.getOrganizationId()).isEqualTo(organizationId);
        assertThat(process.isDefaultProcess()).isTrue();
        assertThat(process.getCreatedAt()).isEqualTo(CREATED);
        assertThat(process.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(process.getDeletedAt()).isEqualTo(DELETED);
        assertThat(process.operations())
                .extracting(Operation::number)
                .containsExactly(10, 20);
    }

    @Test
    void restore_activeProcess_keepsNullDeletedAtAndFalseDefault() {
        TechnologyProcess process = TechnologyProcess.restore(
                TechnologyProcessId.generate(), "TP-1", "Имя", PartId.generate(),
                OrganizationId.generate(), false, CREATED, UPDATED, null, List.of());

        assertThat(process.getDeletedAt()).isNull();
        assertThat(process.isDefaultProcess()).isFalse();
    }

    @Test
    void restore_preservesOperationEquipmentIdAndOrdersByNumber() {
        EquipmentId equipmentId = EquipmentId.generate();
        TechnologyProcess process = TechnologyProcess.restore(
                TechnologyProcessId.generate(), "TP-1", "Фрезеровка", PartId.generate(),
                OrganizationId.generate(), false, CREATED, UPDATED, null,
                List.of(
                        new Operation(20, "Б"),
                        new Operation(10, "А", equipmentId)));

        assertThat(process.operations())
                .extracting(Operation::number)
                .containsExactly(10, 20);
        assertThat(process.operations().get(0).equipmentId()).isEqualTo(equipmentId);
        assertThat(process.operations().get(1).equipmentId()).isNull();
    }
}
