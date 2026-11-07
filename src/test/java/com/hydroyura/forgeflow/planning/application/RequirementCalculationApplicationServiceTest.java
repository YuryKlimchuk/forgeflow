package com.hydroyura.forgeflow.planning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationView;
import com.hydroyura.forgeflow.planning.domain.Plan;
import com.hydroyura.forgeflow.planning.domain.PlanNotFoundException;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculationNotFoundException;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculator;
import com.hydroyura.forgeflow.planning.domain.RequirementCycleException;
import com.hydroyura.forgeflow.planning.domain.RequirementQuantityOverflowException;
import com.hydroyura.forgeflow.planning.domain.SpecificationItemNotFoundException;
import com.hydroyura.forgeflow.planning.infrastructure.FakeRequirementSource;
import com.hydroyura.forgeflow.planning.infrastructure.InMemoryPlanRepository;
import com.hydroyura.forgeflow.planning.infrastructure.InMemoryRequirementCalculationRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequirementCalculationApplicationServiceTest {

    private static final Instant T1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-02T00:00:00Z");

    private final OrganizationId org = OrganizationId.generate();
    private final OrganizationId otherOrg = OrganizationId.generate();
    private final InMemoryPlanRepository planRepository = new InMemoryPlanRepository();
    private final InMemoryRequirementCalculationRepository repository =
            new InMemoryRequirementCalculationRepository();
    private final Clock clock = mock(Clock.class);
    private final RequirementCalculationApplicationService service =
            new RequirementCalculationApplicationService(
                    repository, planRepository, fakeContext(org), new RequirementCalculator(),
                    new FakeRequirementSource(), clock, new RequirementCalculationMapper());

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(T1);
    }

    private Plan savedPlan(OrganizationId owner, AssemblyId assembly, int quantity) {
        Plan plan = Plan.create(PlanId.generate(), owner, "План", 2027, 1);
        plan.addItem(assembly, quantity);
        planRepository.save(plan);
        return plan;
    }

    @Test
    void calculateCreatesAndSavesCalculation() {
        AssemblyId a = AssemblyId.generate();
        UUID part = UUID.randomUUID();
        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, 3));
        Plan plan = savedPlan(org, a, 2);

        RequirementCalculationApplicationService svc = new RequirementCalculationApplicationService(
                repository, planRepository, fakeContext(org), new RequirementCalculator(),
                source, clock, new RequirementCalculationMapper());

        RequirementCalculationView view = svc.calculate(plan.getId());

        assertThat(view.planId()).isEqualTo(plan.getId());
        assertThat(view.executedAt()).isEqualTo(T1);
        assertThat(view.items()).hasSize(2);
        assertThat(repository.findByPlanId(org, plan.getId())).hasSize(1);
    }

    @Test
    void historyDoesNotOverwrite() {
        AssemblyId a = AssemblyId.generate();
        FakeRequirementSource source = new FakeRequirementSource().assembly(a, "СБ-0001", "Насос");
        Plan plan = savedPlan(org, a, 1);

        RequirementCalculationApplicationService svc = new RequirementCalculationApplicationService(
                repository, planRepository, fakeContext(org), new RequirementCalculator(),
                source, clock, new RequirementCalculationMapper());

        when(clock.instant()).thenReturn(T1, T2);
        svc.calculate(plan.getId());
        svc.calculate(plan.getId());

        assertThat(svc.list(plan.getId())).hasSize(2);
        assertThat(svc.list(plan.getId())).extracting(RequirementCalculationView::executedAt)
                .containsExactly(T2, T1);
    }

    @Test
    void snapshotDoesNotChangeAfterSourceMutation() {
        AssemblyId a = AssemblyId.generate();
        UUID part = UUID.randomUUID();
        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, 3));
        Plan plan = savedPlan(org, a, 2);

        RequirementCalculationApplicationService svc = new RequirementCalculationApplicationService(
                repository, planRepository, fakeContext(org), new RequirementCalculator(),
                source, clock, new RequirementCalculationMapper());

        RequirementCalculationView view = svc.calculate(plan.getId());

        source.descriptor(CatalogItemType.PART, part, "Д-0001", "Изменённая");
        source.rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, 99));

        RequirementCalculationView reloaded = svc.get(plan.getId(), view.id());

        assertThat(reloaded.items())
                .anySatisfy(item -> {
                    assertThat(item.id()).isEqualTo(part);
                    assertThat(item.name()).isEqualTo("Втулка");
                    assertThat(item.quantity()).isEqualTo(6);
                });
    }

    @Test
    void foreignOrMissingPlanThrowsNotFound() {
        AssemblyId a = AssemblyId.generate();
        Plan foreignPlan = savedPlan(otherOrg, a, 1);

        assertThatThrownBy(() -> service.calculate(foreignPlan.getId()))
                .isInstanceOf(PlanNotFoundException.class);
        assertThatThrownBy(() -> service.list(foreignPlan.getId()))
                .isInstanceOf(PlanNotFoundException.class);
        assertThatThrownBy(() -> service.get(foreignPlan.getId(), RequirementCalculationId.generate()))
                .isInstanceOf(PlanNotFoundException.class);
        assertThatThrownBy(() -> service.list(PlanId.generate()))
                .isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void emptyPlanReturnsEmptyCalculation() {
        Plan plan = Plan.create(PlanId.generate(), org, "Пустой", 2027, 1);
        planRepository.save(plan);

        RequirementCalculationView view = service.calculate(plan.getId());

        assertThat(view.items()).isEmpty();
        assertThat(repository.findByPlanId(org, plan.getId())).hasSize(1);
    }

    @Test
    void getMissingCalculationThrowsNotFound() {
        AssemblyId a = AssemblyId.generate();
        Plan plan = savedPlan(org, a, 1);

        assertThatThrownBy(() -> service.get(plan.getId(), RequirementCalculationId.generate()))
                .isInstanceOf(RequirementCalculationNotFoundException.class);
    }

    @Test
    void cycleDoesNotSavePartialResult() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();
        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "A")
                .assembly(b, "СБ-0002", "B")
                .rows(a, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, b.value(), 1))
                .rows(b, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, a.value(), 1));
        Plan plan = savedPlan(org, a, 1);

        RequirementCalculationApplicationService svc = new RequirementCalculationApplicationService(
                repository, planRepository, fakeContext(org), new RequirementCalculator(),
                source, clock, new RequirementCalculationMapper());

        assertThatThrownBy(() -> svc.calculate(plan.getId()))
                .isInstanceOf(RequirementCycleException.class);
        assertThat(repository.findByPlanId(org, plan.getId())).isEmpty();
    }

    @Test
    void overflowDoesNotSavePartialResult() {
        AssemblyId a = AssemblyId.generate();
        UUID part = UUID.randomUUID();
        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .descriptor(CatalogItemType.PART, part, "Д-0001", "Втулка")
                .rows(a, FakeRequirementSource.row(CatalogItemType.PART, part, 2));
        Plan plan = savedPlan(org, a, Integer.MAX_VALUE);

        RequirementCalculationApplicationService svc = new RequirementCalculationApplicationService(
                repository, planRepository, fakeContext(org), new RequirementCalculator(),
                source, clock, new RequirementCalculationMapper());

        assertThatThrownBy(() -> svc.calculate(plan.getId()))
                .isInstanceOf(RequirementQuantityOverflowException.class);
        assertThat(repository.findByPlanId(org, plan.getId())).isEmpty();
    }

    @Test
    void danglingNestedAssemblyDoesNotSavePartialResult() {
        AssemblyId a = AssemblyId.generate();
        AssemblyId b = AssemblyId.generate();
        FakeRequirementSource source = new FakeRequirementSource()
                .assembly(a, "СБ-0001", "Насос")
                .rows(a, FakeRequirementSource.row(CatalogItemType.ASSEMBLY, b.value(), 1));
        Plan plan = savedPlan(org, a, 1);

        RequirementCalculationApplicationService svc = new RequirementCalculationApplicationService(
                repository, planRepository, fakeContext(org), new RequirementCalculator(),
                source, clock, new RequirementCalculationMapper());

        assertThatThrownBy(() -> svc.calculate(plan.getId()))
                .isInstanceOf(SpecificationItemNotFoundException.class);
        assertThat(repository.findByPlanId(org, plan.getId())).isEmpty();
    }

    @Test
    void listReturnsSavedCalculations() {
        AssemblyId a = AssemblyId.generate();
        FakeRequirementSource source = new FakeRequirementSource().assembly(a, "СБ-0001", "Насос");
        Plan plan = savedPlan(org, a, 1);

        RequirementCalculationApplicationService svc = new RequirementCalculationApplicationService(
                repository, planRepository, fakeContext(org), new RequirementCalculator(),
                source, clock, new RequirementCalculationMapper());

        RequirementCalculationView view = svc.calculate(plan.getId());

        assertThat(svc.list(plan.getId())).containsExactly(view);
    }

    private static OrganizationContext fakeContext(OrganizationId id) {
        return new OrganizationContext() {
            @Override
            public Optional<OrganizationId> currentOrganizationId() {
                return Optional.ofNullable(id);
            }

            @Override
            public OrganizationId requiredOrganizationId() {
                if (id == null) {
                    throw new OrganizationContextNotAvailableException("no organization context");
                }
                return id;
            }
        };
    }
}
