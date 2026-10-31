package com.hydroyura.forgeflow.planning.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.domain.Plan;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import org.junit.jupiter.api.Test;

class InMemoryPlanRepositoryTest {

    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();

    private Plan plan(OrganizationId orgId, String name) {
        return Plan.create(PlanId.generate(), orgId, name, 2027, 12);
    }

    @Test
    void saveFindListAndIsolation() {
        OrganizationId orgA = OrganizationId.generate();
        OrganizationId orgB = OrganizationId.generate();
        Plan planA = plan(orgA, "План A");
        repository.save(planA);

        assertThat(repository.findById(orgA, planA.getId())).contains(planA);
        assertThat(repository.findById(orgB, planA.getId())).isEmpty();
        assertThat(repository.findAll(orgA)).containsExactly(planA);
        assertThat(repository.findAll(orgB)).isEmpty();
        assertThat(repository.existsByName(orgA, "План A")).isTrue();
        assertThat(repository.existsByName(orgB, "План A")).isFalse();
    }
}
