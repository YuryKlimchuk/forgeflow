package com.hydroyura.forgeflow.technology.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.OperationView;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessView;
import com.hydroyura.forgeflow.technology.domain.Operation;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TechnologyMapperTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant DELETED = Instant.parse("2026-03-01T00:00:00Z");

    @Test
    void toView_mapsAllFieldsAndKeepsOperationOrder() {
        TechnologyProcessId id = TechnologyProcessId.generate();
        PartId partId = PartId.generate();
        OrganizationId organizationId = OrganizationId.generate();
        TechnologyProcess process = TechnologyProcess.restore(
                id, "TP-1", "Фрезеровка", partId, organizationId, true,
                CREATED, UPDATED, DELETED,
                List.of(new Operation(20, "Б"), new Operation(10, "А")));

        TechnologyProcessView view = new TechnologyMapper().toView(process);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.number()).isEqualTo("TP-1");
        assertThat(view.name()).isEqualTo("Фрезеровка");
        assertThat(view.partId()).isEqualTo(partId);
        assertThat(view.defaultProcess()).isTrue();
        assertThat(view.createdAt()).isEqualTo(CREATED);
        assertThat(view.updatedAt()).isEqualTo(UPDATED);
        assertThat(view.operations())
                .extracting(OperationView::number)
                .containsExactly(10, 20);
    }
}
