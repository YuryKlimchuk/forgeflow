package com.hydroyura.forgeflow.planning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.application.AssemblyNotFoundException;
import com.hydroyura.forgeflow.planning.api.AddPlanItemRequest;
import com.hydroyura.forgeflow.planning.api.ChangePlanItemQuantityRequest;
import com.hydroyura.forgeflow.planning.api.CreatePlanRequest;
import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.PlanView;
import com.hydroyura.forgeflow.planning.domain.DuplicatePlanItemException;
import com.hydroyura.forgeflow.planning.domain.DuplicatePlanNameException;
import com.hydroyura.forgeflow.planning.domain.PlanItemNotFoundException;
import com.hydroyura.forgeflow.planning.domain.PlanNotFoundException;
import com.hydroyura.forgeflow.planning.infrastructure.InMemoryPlanRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.FlatBomItem;
import com.hydroyura.forgeflow.specifications.api.SpecificationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgId = OrganizationId.generate();
    private final InMemoryPlanRepository repository = new InMemoryPlanRepository();
    private final AssemblyService assemblyService = mock(AssemblyService.class);
    private final SpecificationService specificationService = mock(SpecificationService.class);
    private final PlanApplicationService service = new PlanApplicationService(
            repository, fakeContext(orgId), assemblyService, specificationService, new PlanMapper());

    private final AssemblyId assemblyA = AssemblyId.generate();
    private final AssemblyId assemblyB = AssemblyId.generate();

    @BeforeEach
    void setUp() {
        when(assemblyService.get(assemblyA)).thenReturn(new AssemblyView(assemblyA, "СБ-0001", "Насос", NOW, NOW));
        when(assemblyService.get(assemblyB)).thenReturn(new AssemblyView(assemblyB, "СБ-0002", "Пульт", NOW, NOW));
    }

    @Test
    void createAndGet() {
        PlanView created = service.create(new CreatePlanRequest("Декабрь 2027", 2027, 12));

        assertThat(created.name()).isEqualTo("Декабрь 2027");
        assertThat(created.year()).isEqualTo(2027);
        assertThat(created.month()).isEqualTo(12);
        assertThat(created.items()).isEmpty();
        assertThat(service.get(created.id())).isEqualTo(created);
    }

    @Test
    void createRejectsDuplicateName() {
        service.create(new CreatePlanRequest("План", 2027, 12));

        assertThatThrownBy(() -> service.create(new CreatePlanRequest("План", 2027, 12)))
                .isInstanceOf(DuplicatePlanNameException.class);
    }

    @Test
    void listReturnsPlansOfOrganization() {
        service.create(new CreatePlanRequest("План A", 2027, 1));
        service.create(new CreatePlanRequest("План B", 2027, 2));

        assertThat(service.list()).hasSize(2);
    }

    @Test
    void addItemAndChangeQuantityAndRemove() {
        PlanView plan = service.create(new CreatePlanRequest("План", 2027, 12));

        service.addItem(plan.id(), new AddPlanItemRequest(assemblyA, 2));
        assertThat(service.get(plan.id()).items()).hasSize(1);

        service.changeQuantity(plan.id(), assemblyA, new ChangePlanItemQuantityRequest(5));
        assertThat(service.get(plan.id()).items().get(0).quantity()).isEqualTo(5);

        service.removeItem(plan.id(), assemblyA);
        assertThat(service.get(plan.id()).items()).isEmpty();
    }

    @Test
    void addItemRejectsDuplicateAssembly() {
        PlanView plan = service.create(new CreatePlanRequest("План", 2027, 12));
        service.addItem(plan.id(), new AddPlanItemRequest(assemblyA, 2));

        assertThatThrownBy(() -> service.addItem(plan.id(), new AddPlanItemRequest(assemblyA, 3)))
                .isInstanceOf(DuplicatePlanItemException.class);
    }

    @Test
    void addItemRejectsForeignAssembly() {
        AssemblyId foreign = AssemblyId.generate();
        when(assemblyService.get(foreign)).thenThrow(new AssemblyNotFoundException(foreign));
        PlanView plan = service.create(new CreatePlanRequest("План", 2027, 12));

        assertThatThrownBy(() -> service.addItem(plan.id(), new AddPlanItemRequest(foreign, 1)))
                .isInstanceOf(AssemblyNotFoundException.class);
    }

    @Test
    void changeQuantityRejectsMissingItem() {
        PlanView plan = service.create(new CreatePlanRequest("План", 2027, 12));

        assertThatThrownBy(() -> service.changeQuantity(plan.id(), assemblyA, new ChangePlanItemQuantityRequest(1)))
                .isInstanceOf(PlanItemNotFoundException.class);
    }

    @Test
    void flattenSumsAcrossAssemblies() {
        when(specificationService.flatten(assemblyA))
                .thenReturn(List.of(new FlatBomItem("Д-0001", "Втулка", 3)));
        when(specificationService.flatten(assemblyB))
                .thenReturn(List.of(new FlatBomItem("Д-0001", "Втулка", 4)));

        PlanView plan = service.create(new CreatePlanRequest("План", 2027, 12));
        service.addItem(plan.id(), new AddPlanItemRequest(assemblyA, 2));
        service.addItem(plan.id(), new AddPlanItemRequest(assemblyB, 1));

        List<FlatBomItem> result = service.flatten(plan.id());

        assertThat(result).containsExactly(new FlatBomItem("Д-0001", "Втулка", 10));
    }

    @Test
    void getMissingPlanThrowsNotFound() {
        assertThatThrownBy(() -> service.get(PlanId.generate()))
                .isInstanceOf(PlanNotFoundException.class);
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
