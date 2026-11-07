package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.EquipmentId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.domain.Operation;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TechnologyMapperTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant DELETED = Instant.parse("2026-03-01T00:00:00Z");

    private final TechnologyMapper mapper = TechnologyMapper.INSTANCE;

    @Test
    void entityRoundTrip_preservesAllFieldsAndOrderedOperations() {
        TechnologyProcessId id = TechnologyProcessId.generate();
        PartId partId = PartId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        TechnologyProcess process = TechnologyProcess.restore(
                id, "TP-1", "Фрезеровка", partId, organizationId, true,
                CREATED, UPDATED, DELETED,
                List.of(new Operation(20, "Б"), new Operation(10, "А")));

        TechnologyProcessEntity entity = mapper.toEntity(process);
        List<TechnologyProcessOperationEntity> operationEntities = mapper.toOperationEntities(process);
        TechnologyProcess restored = mapper.toDomain(entity, operationEntities);

        assertThat(restored.getId()).isEqualTo(id);
        assertThat(restored.getNumber()).isEqualTo("TP-1");
        assertThat(restored.getName()).isEqualTo("Фрезеровка");
        assertThat(restored.getPartId()).isEqualTo(partId);
        assertThat(restored.getOrganizationId()).isEqualTo(organizationId);
        assertThat(restored.isDefaultProcess()).isTrue();
        assertThat(restored.getCreatedAt()).isEqualTo(CREATED);
        assertThat(restored.getUpdatedAt()).isEqualTo(UPDATED);
        assertThat(restored.getDeletedAt()).isEqualTo(DELETED);
        assertThat(restored.operations())
                .extracting(Operation::number)
                .containsExactly(10, 20);
    }

    @Test
    void toOperationEntities_usesProcessIdAndOrderedOperations() {
        TechnologyProcess process = TechnologyProcess.restore(
                TechnologyProcessId.generate(), "TP-1", "Имя", PartId.generate(),
                OrganizationId.generate(), false, CREATED, UPDATED, null,
                List.of(new Operation(20, "Б"), new Operation(10, "А")));

        List<TechnologyProcessOperationEntity> entities = mapper.toOperationEntities(process);

        assertThat(entities).extracting(e -> e.operationNumber).containsExactly(10, 20);
        assertThat(entities).allSatisfy(e -> assertThat(e.processId).isEqualTo(process.getId().value()));
    }

    @Test
    void operationEquipmentIdRoundTrip_preservesNullAndNonNull() {
        EquipmentId equipmentId = EquipmentId.generate();
        TechnologyProcess process = TechnologyProcess.restore(
                TechnologyProcessId.generate(), "TP-1", "Имя", PartId.generate(),
                OrganizationId.generate(), false, CREATED, UPDATED, null,
                List.of(
                        new Operation(20, "Б"),
                        new Operation(10, "А", equipmentId)));

        List<TechnologyProcessOperationEntity> entities = mapper.toOperationEntities(process);
        TechnologyProcess restored = mapper.toDomain(mapper.toEntity(process), entities);

        assertThat(entities).extracting(e -> e.equipmentId)
                .containsExactly(equipmentId.value(), null);
        assertThat(restored.operations())
                .extracting(Operation::equipmentId)
                .containsExactly(equipmentId, null);
    }
}
