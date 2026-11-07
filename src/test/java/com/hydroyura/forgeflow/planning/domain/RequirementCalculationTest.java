package com.hydroyura.forgeflow.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.NeedType;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequirementCalculationTest {

    private static final Instant EXECUTED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private static RequirementItem item() {
        return new RequirementItem(
                CatalogItemType.PART, UUID.randomUUID(), "Д-0001", "Втулка", 2,
                NeedType.MANUFACTURE, "шт", List.of(AssemblyId.generate()));
    }

    private RequirementCalculation calculation(List<RequirementItem> items) {
        return RequirementCalculation.create(
                RequirementCalculationId.generate(),
                OrganizationId.generate(),
                PlanId.generate(),
                EXECUTED_AT,
                items);
    }

    @Test
    void holdsAllFields() {
        RequirementItem item = item();
        RequirementCalculationId id = RequirementCalculationId.generate();
        OrganizationId org = OrganizationId.generate();
        PlanId planId = PlanId.generate();

        RequirementCalculation calculation = RequirementCalculation.create(
                id, org, planId, EXECUTED_AT, List.of(item));

        assertThat(calculation.getId()).isEqualTo(id);
        assertThat(calculation.getOrganizationId()).isEqualTo(org);
        assertThat(calculation.getPlanId()).isEqualTo(planId);
        assertThat(calculation.getExecutedAt()).isEqualTo(EXECUTED_AT);
        assertThat(calculation.items()).containsExactly(item);
    }

    @Test
    void allowsEmptyItems() {
        RequirementCalculation calculation = calculation(List.of());

        assertThat(calculation.items()).isEmpty();
    }

    @Test
    void itemsAreImmutable() {
        RequirementCalculation calculation = calculation(List.of(item()));

        assertThatThrownBy(() -> calculation.items().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> RequirementCalculation.create(
                null, OrganizationId.generate(), PlanId.generate(), EXECUTED_AT, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RequirementCalculation.create(
                RequirementCalculationId.generate(), null, PlanId.generate(), EXECUTED_AT, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RequirementCalculation.create(
                RequirementCalculationId.generate(), OrganizationId.generate(), null, EXECUTED_AT, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RequirementCalculation.create(
                RequirementCalculationId.generate(), OrganizationId.generate(), PlanId.generate(), null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
