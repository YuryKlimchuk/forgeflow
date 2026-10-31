package com.hydroyura.forgeflow.planning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import org.junit.jupiter.api.Test;

class PlanTest {

    private final OrganizationId orgId = OrganizationId.generate();

    private Plan plan() {
        return Plan.create(PlanId.generate(), orgId, "Декабрь 2027", 2027, 12);
    }

    @Test
    void createWithValidPeriodAndEmptyItems() {
        Plan plan = plan();

        assertThat(plan.getName()).isEqualTo("Декабрь 2027");
        assertThat(plan.getYear()).isEqualTo(2027);
        assertThat(plan.getMonth()).isEqualTo(12);
        assertThat(plan.items()).isEmpty();
    }

    @Test
    void rejectsInvalidMonth() {
        assertThatThrownBy(() -> Plan.create(PlanId.generate(), orgId, "План", 2027, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Plan.create(PlanId.generate(), orgId, "План", 2027, 13))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Plan.create(PlanId.generate(), orgId, "  ", 2027, 12))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addItemAndRejectDuplicateAndInvalidQuantity() {
        Plan plan = plan();
        AssemblyId assemblyId = AssemblyId.generate();

        plan.addItem(assemblyId, 3);
        assertThat(plan.items()).hasSize(1);
        assertThat(plan.items().get(0).quantity()).isEqualTo(3);

        assertThatThrownBy(() -> plan.addItem(assemblyId, 5))
                .isInstanceOf(DuplicatePlanItemException.class);
        assertThatThrownBy(() -> plan.addItem(AssemblyId.generate(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeQuantityAndRemoveItem() {
        Plan plan = plan();
        AssemblyId assemblyId = AssemblyId.generate();
        plan.addItem(assemblyId, 2);

        plan.changeQuantity(assemblyId, 5);
        assertThat(plan.items().get(0).quantity()).isEqualTo(5);

        plan.removeItem(assemblyId);
        assertThat(plan.items()).isEmpty();

        assertThatThrownBy(() -> plan.changeQuantity(assemblyId, 1))
                .isInstanceOf(PlanItemNotFoundException.class);
        assertThatThrownBy(() -> plan.removeItem(assemblyId))
                .isInstanceOf(PlanItemNotFoundException.class);
    }
}
