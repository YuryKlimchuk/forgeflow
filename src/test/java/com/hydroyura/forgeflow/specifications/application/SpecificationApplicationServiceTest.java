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
import com.hydroyura.forgeflow.catalog.api.CreateAssemblyRequest;
import com.hydroyura.forgeflow.catalog.application.AssemblyNotFoundException;
import com.hydroyura.forgeflow.specifications.api.AddRowRequest;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.api.CatalogItemType;
import com.hydroyura.forgeflow.specifications.api.ChangeQuantityRequest;
import com.hydroyura.forgeflow.specifications.api.CopySpecificationRequest;
import com.hydroyura.forgeflow.specifications.api.RemoveRowRequest;
import com.hydroyura.forgeflow.specifications.api.SpecificationRowView;
import com.hydroyura.forgeflow.specifications.domain.DuplicateItemException;
import com.hydroyura.forgeflow.specifications.domain.RowNotFoundException;
import com.hydroyura.forgeflow.specifications.domain.SelfReferenceException;
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
    private final SpecificationApplicationService service = new SpecificationApplicationService(
            repository, fakeContext(orgId), assemblyService, new SpecificationMapper());

    @BeforeEach
    void setUp() {
        when(assemblyService.get(assemblyId)).thenReturn(assemblyView(assemblyId, "A-1"));
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
                .thenReturn(assemblyView(newAssemblyId, "B-1"));

        AssemblyView result = service.copy(assemblyId, new CopySpecificationRequest("B-1", "Новая"));

        assertThat(result.id()).isEqualTo(newAssemblyId);
        List<SpecificationRowView> copiedRows = service.get(newAssemblyId);
        assertThat(copiedRows).containsExactly(new SpecificationRowView(item, 7));
    }

    @Test
    void copyEmptySpecificationCreatesEmptyAssembly() {
        AssemblyId newAssemblyId = AssemblyId.generate();
        when(assemblyService.create(any(CreateAssemblyRequest.class)))
                .thenReturn(assemblyView(newAssemblyId, "B-2"));

        service.copy(assemblyId, new CopySpecificationRequest("B-2", "Новая"));

        assertThat(service.get(newAssemblyId)).isEmpty();
    }

    @Test
    void copyForeignSourceDoesNotCreateAssembly() {
        AssemblyId foreign = AssemblyId.generate();
        when(assemblyService.get(foreign)).thenThrow(new AssemblyNotFoundException(foreign));

        assertThatThrownBy(() -> service.copy(foreign, new CopySpecificationRequest("B-3", "Новая")))
                .isInstanceOf(AssemblyNotFoundException.class);
        verify(assemblyService, never()).create(any(CreateAssemblyRequest.class));
    }

    @Test
    void tenantIsolationBetweenOrganizations() {
        InMemorySpecificationRepository shared = new InMemorySpecificationRepository();
        SpecificationApplicationService serviceA = new SpecificationApplicationService(
                shared, fakeContext(orgId), assemblyService, new SpecificationMapper());
        OrganizationId orgB = OrganizationId.generate();
        SpecificationApplicationService serviceB = new SpecificationApplicationService(
                shared, fakeContext(orgB), assemblyService, new SpecificationMapper());
        when(assemblyService.get(assemblyId)).thenReturn(assemblyView(assemblyId, "A-1"));
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
