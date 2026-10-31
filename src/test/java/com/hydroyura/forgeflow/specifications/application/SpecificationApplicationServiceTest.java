package com.hydroyura.forgeflow.specifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.api.BuyService;
import com.hydroyura.forgeflow.catalog.api.CreateAssemblyRequest;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.catalog.api.PartView;
import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.api.StandartService;
import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.api.VzkService;
import com.hydroyura.forgeflow.catalog.application.AssemblyNotFoundException;
import com.hydroyura.forgeflow.specifications.api.AddRowRequest;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.api.ChangeQuantityRequest;
import com.hydroyura.forgeflow.specifications.api.CopySpecificationRequest;
import com.hydroyura.forgeflow.specifications.api.FlatBomItem;
import com.hydroyura.forgeflow.specifications.api.RemoveRowRequest;
import com.hydroyura.forgeflow.specifications.api.SpecificationRowView;
import com.hydroyura.forgeflow.specifications.domain.BomCycleException;
import com.hydroyura.forgeflow.specifications.domain.DuplicateItemException;
import com.hydroyura.forgeflow.specifications.domain.Quantity;
import com.hydroyura.forgeflow.specifications.domain.RowNotFoundException;
import com.hydroyura.forgeflow.specifications.domain.SelfReferenceException;
import com.hydroyura.forgeflow.specifications.domain.Specification;
import com.hydroyura.forgeflow.specifications.infrastructure.InMemorySpecificationRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpecificationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizationId orgId = OrganizationId.generate();
    private final AssemblyId assemblyId = AssemblyId.generate();
    private final InMemorySpecificationRepository repository = new InMemorySpecificationRepository();
    private final AssemblyService assemblyService = mock(AssemblyService.class);
    private final PartService partService = mock(PartService.class);
    private final BuyService buyService = mock(BuyService.class);
    private final StandartService standartService = mock(StandartService.class);
    private final VzkService vzkService = mock(VzkService.class);
    private final SpecificationApplicationService service = new SpecificationApplicationService(
            repository, fakeContext(orgId), assemblyService,
            partService, buyService, standartService, vzkService, new SpecificationMapper());

    @BeforeEach
    void setUp() {
        when(assemblyService.get(assemblyId)).thenReturn(assemblyView(assemblyId, "СБ-0001"));
    }

    private static CatalogItemReference part(String id) {
        return new CatalogItemReference(CatalogItemType.PART, UUID.fromString(id));
    }

    private static AssemblyView assemblyView(AssemblyId id, String number) {
        return new AssemblyView(id, number, "Имя", NOW, NOW);
    }

    @Test
    void getReturnsEmptyForNoRows() {
        assertThat(service.get(assemblyId)).isEmpty();
    }

    @Test
    void addRowAndGetReturnRow() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000001");

        SpecificationRowView view = service.addRow(assemblyId, new AddRowRequest(item, 3));

        assertThat(view.item()).isEqualTo(item);
        assertThat(view.quantity()).isEqualTo(3);
        assertThat(service.get(assemblyId)).containsExactly(view);
    }

    @Test
    void addRowRejectsDuplicate() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000002");
        service.addRow(assemblyId, new AddRowRequest(item, 1));

        assertThatThrownBy(() -> service.addRow(assemblyId, new AddRowRequest(item, 5)))
                .isInstanceOf(DuplicateItemException.class);
    }

    @Test
    void addRowRejectsSelfReference() {
        CatalogItemReference self = new CatalogItemReference(CatalogItemType.ASSEMBLY, assemblyId.value());

        assertThatThrownBy(() -> service.addRow(assemblyId, new AddRowRequest(self, 1)))
                .isInstanceOf(SelfReferenceException.class);
    }

    @Test
    void addRowRejectsNonPositiveQuantity() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000003");

        assertThatThrownBy(() -> service.addRow(assemblyId, new AddRowRequest(item, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeQuantityUpdatesRow() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000004");
        service.addRow(assemblyId, new AddRowRequest(item, 1));

        service.changeQuantity(assemblyId, new ChangeQuantityRequest(item, 9));

        assertThat(service.get(assemblyId).get(0).quantity()).isEqualTo(9);
    }

    @Test
    void changeQuantityRejectsMissingRow() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000005");

        assertThatThrownBy(() -> service.changeQuantity(assemblyId, new ChangeQuantityRequest(item, 2)))
                .isInstanceOf(RowNotFoundException.class);
    }

    @Test
    void removeRowRemoves() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000006");
        service.addRow(assemblyId, new AddRowRequest(item, 1));

        service.removeRow(assemblyId, new RemoveRowRequest(item));

        assertThat(service.get(assemblyId)).isEmpty();
    }

    @Test
    void removeRowRejectsMissingRow() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000007");

        assertThatThrownBy(() -> service.removeRow(assemblyId, new RemoveRowRequest(item)))
                .isInstanceOf(RowNotFoundException.class);
    }

    @Test
    void getForeignAssemblyThrowsNotFound() {
        AssemblyId foreign = AssemblyId.generate();
        when(assemblyService.get(foreign)).thenThrow(new AssemblyNotFoundException(foreign));

        assertThatThrownBy(() -> service.get(foreign))
                .isInstanceOf(AssemblyNotFoundException.class);
    }

    @Test
    void copyCreatesAssemblyAndCopiesRows() {
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000008");
        service.addRow(assemblyId, new AddRowRequest(item, 7));
        AssemblyId newAssemblyId = AssemblyId.generate();
        when(assemblyService.create(any(CreateAssemblyRequest.class)))
                .thenReturn(assemblyView(newAssemblyId, "СБ-0002"));

        AssemblyView result = service.copy(assemblyId, new CopySpecificationRequest("СБ-0002", "Новая"));

        assertThat(result.id()).isEqualTo(newAssemblyId);
        List<SpecificationRowView> copiedRows = service.get(newAssemblyId);
        assertThat(copiedRows).containsExactly(new SpecificationRowView(item, 7));
    }

    @Test
    void copyForeignSourceDoesNotCreateAssembly() {
        AssemblyId foreign = AssemblyId.generate();
        when(assemblyService.get(foreign)).thenThrow(new AssemblyNotFoundException(foreign));

        assertThatThrownBy(() -> service.copy(foreign, new CopySpecificationRequest("СБ-0003", "Новая")))
                .isInstanceOf(AssemblyNotFoundException.class);
        verify(assemblyService, never()).create(any(CreateAssemblyRequest.class));
    }

    @Test
    void flattenMultipliesAndSums() {
        PartId partId = PartId.generate();
        CatalogItemReference x = new CatalogItemReference(CatalogItemType.PART, partId.value());
        when(partService.get(partId)).thenReturn(new PartView(partId, "Д-0001", "Втулка", NOW, NOW));

        AssemblyId assemblyB = AssemblyId.generate();
        when(assemblyService.get(assemblyB)).thenReturn(assemblyView(assemblyB, "СБ-0002"));

        Specification specA = Specification.create(orgId, assemblyId);
        specA.addRow(new CatalogItemReference(CatalogItemType.ASSEMBLY, assemblyB.value()), new Quantity(2));
        specA.addRow(x, new Quantity(3));
        repository.save(specA);

        Specification specB = Specification.create(orgId, assemblyB);
        specB.addRow(x, new Quantity(4));
        repository.save(specB);

        List<FlatBomItem> result = service.flatten(assemblyId);

        assertThat(result).containsExactly(new FlatBomItem("Д-0001", "Втулка", 11));
    }

    @Test
    void flattenSortsByNumber() {
        PartId first = PartId.generate();
        PartId second = PartId.generate();
        when(partService.get(first)).thenReturn(new PartView(first, "Д-0002", "Вторая", NOW, NOW));
        when(partService.get(second)).thenReturn(new PartView(second, "Д-0001", "Первая", NOW, NOW));

        Specification spec = Specification.create(orgId, assemblyId);
        spec.addRow(new CatalogItemReference(CatalogItemType.PART, first.value()), new Quantity(1));
        spec.addRow(new CatalogItemReference(CatalogItemType.PART, second.value()), new Quantity(1));
        repository.save(spec);

        List<FlatBomItem> result = service.flatten(assemblyId);

        assertThat(result).extracting(FlatBomItem::number).containsExactly("Д-0001", "Д-0002");
    }

    @Test
    void flattenDetectsCycle() {
        AssemblyId assemblyB = AssemblyId.generate();
        AssemblyId assemblyC = AssemblyId.generate();
        when(assemblyService.get(assemblyB)).thenReturn(assemblyView(assemblyB, "СБ-0002"));
        when(assemblyService.get(assemblyC)).thenReturn(assemblyView(assemblyC, "СБ-0003"));

        Specification a = Specification.create(orgId, assemblyId);
        a.addRow(new CatalogItemReference(CatalogItemType.ASSEMBLY, assemblyB.value()), new Quantity(1));
        repository.save(a);
        Specification b = Specification.create(orgId, assemblyB);
        b.addRow(new CatalogItemReference(CatalogItemType.ASSEMBLY, assemblyC.value()), new Quantity(1));
        repository.save(b);
        Specification c = Specification.create(orgId, assemblyC);
        c.addRow(new CatalogItemReference(CatalogItemType.ASSEMBLY, assemblyId.value()), new Quantity(1));
        repository.save(c);

        assertThatThrownBy(() -> service.flatten(assemblyId))
                .isInstanceOf(BomCycleException.class);
    }

    @Test
    void tenantIsolationBetweenOrganizations() {
        InMemorySpecificationRepository shared = new InMemorySpecificationRepository();
        SpecificationApplicationService serviceA = new SpecificationApplicationService(
                shared, fakeContext(orgId), assemblyService,
                partService, buyService, standartService, vzkService, new SpecificationMapper());
        OrganizationId orgB = OrganizationId.generate();
        SpecificationApplicationService serviceB = new SpecificationApplicationService(
                shared, fakeContext(orgB), assemblyService,
                partService, buyService, standartService, vzkService, new SpecificationMapper());
        CatalogItemReference item = part("00000000-0000-0000-0000-000000000009");

        serviceA.addRow(assemblyId, new AddRowRequest(item, 1));

        assertThat(serviceB.get(assemblyId)).isEmpty();
        assertThat(serviceA.get(assemblyId)).hasSize(1);
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
