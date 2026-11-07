package com.hydroyura.forgeflow.planning.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationView;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculation;
import com.hydroyura.forgeflow.planning.domain.RequirementItem;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequirementCalculationMapperTest {

    private static final Instant EXECUTED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private final RequirementCalculationMapper mapper = new RequirementCalculationMapper();

    @Test
    void mapsCalculationAndItems() {
        AssemblyId source = AssemblyId.generate();
        RequirementCalculationId id = RequirementCalculationId.generate();
        PlanId planId = PlanId.generate();
        UUID itemId = UUID.randomUUID();

        RequirementItem item = new RequirementItem(
                CatalogItemType.PART, itemId, "Д-0001", "Втулка", 3,
                NeedType.MANUFACTURE, "шт", List.of(source));
        RequirementCalculation calculation = RequirementCalculation.create(
                id, OrganizationId.generate(), planId, EXECUTED_AT, List.of(item));

        RequirementCalculationView view = mapper.toView(calculation);

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.planId()).isEqualTo(planId);
        assertThat(view.executedAt()).isEqualTo(EXECUTED_AT);
        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).type()).isEqualTo(CatalogItemType.PART);
        assertThat(view.items().get(0).id()).isEqualTo(itemId);
        assertThat(view.items().get(0).number()).isEqualTo("Д-0001");
        assertThat(view.items().get(0).name()).isEqualTo("Втулка");
        assertThat(view.items().get(0).quantity()).isEqualTo(3);
        assertThat(view.items().get(0).needType()).isEqualTo(NeedType.MANUFACTURE);
        assertThat(view.items().get(0).unit()).isEqualTo("шт");
        assertThat(view.items().get(0).sources()).containsExactly(source);
    }
}
