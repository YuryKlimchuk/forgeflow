package com.hydroyura.forgeflow.specifications.application;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.api.CreateAssemblyRequest;
import com.hydroyura.forgeflow.specifications.api.AddRowRequest;
import com.hydroyura.forgeflow.specifications.api.ChangeQuantityRequest;
import com.hydroyura.forgeflow.specifications.api.CopySpecificationRequest;
import com.hydroyura.forgeflow.specifications.api.RemoveRowRequest;
import com.hydroyura.forgeflow.specifications.api.SpecificationRowView;
import com.hydroyura.forgeflow.specifications.api.SpecificationService;
import com.hydroyura.forgeflow.specifications.domain.Quantity;
import com.hydroyura.forgeflow.specifications.domain.RowNotFoundException;
import com.hydroyura.forgeflow.specifications.domain.Specification;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRepository;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRow;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Objects;

/**
 * Реализация {@link SpecificationService}: CRUD прямых строк спецификации и
 * копирование. Текущая организация берётся из {@link OrganizationContext} и
 * передаётся в репозиторий явным аргументом.
 */
public class SpecificationApplicationService implements SpecificationService {

    private final SpecificationRepository repository;
    private final OrganizationContext organizationContext;
    private final AssemblyService assemblyService;
    private final SpecificationMapper mapper;

    public SpecificationApplicationService(
            SpecificationRepository repository,
            OrganizationContext organizationContext,
            AssemblyService assemblyService,
            SpecificationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.assemblyService = Objects.requireNonNull(assemblyService, "assemblyService must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public List<SpecificationRowView> get(AssemblyId assemblyId) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        return repository.findById(organizationId, assemblyId)
                .map(Specification::rows)
                .orElseGet(List::of)
                .stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public SpecificationRowView addRow(AssemblyId assemblyId, AddRowRequest request) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        Quantity quantity = new Quantity(request.quantity());
        Specification specification = repository.findById(organizationId, assemblyId)
                .orElseGet(() -> Specification.create(organizationId, assemblyId));
        specification.addRow(request.item(), quantity);
        repository.save(specification);
        return new SpecificationRowView(request.item(), quantity.value());
    }

    @Override
    public void changeQuantity(AssemblyId assemblyId, ChangeQuantityRequest request) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        Quantity quantity = new Quantity(request.quantity());
        Specification specification = repository.findById(organizationId, assemblyId)
                .orElseThrow(() -> new RowNotFoundException(request.item()));
        specification.changeQuantity(request.item(), quantity);
        repository.save(specification);
    }

    @Override
    public void removeRow(AssemblyId assemblyId, RemoveRowRequest request) {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(assemblyId);
        Specification specification = repository.findById(organizationId, assemblyId)
                .orElseThrow(() -> new RowNotFoundException(request.item()));
        specification.removeRow(request.item());
        repository.save(specification);
    }

    @Override
    public AssemblyView copy(AssemblyId sourceAssemblyId, CopySpecificationRequest request) {
        Objects.requireNonNull(sourceAssemblyId, "sourceAssemblyId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        requireAssembly(sourceAssemblyId);
        List<SpecificationRow> sourceRows = repository.findById(organizationId, sourceAssemblyId)
                .map(Specification::rows)
                .orElseGet(List::of);
        AssemblyView newAssembly = assemblyService.create(new CreateAssemblyRequest(request.number(), request.name()));
        Specification newSpecification = Specification.create(organizationId, newAssembly.id());
        for (SpecificationRow row : sourceRows) {
            newSpecification.addRow(row.item(), row.quantity());
        }
        repository.save(newSpecification);
        return newAssembly;
    }

    /**
     * Проверяет, что сборка существует, активна и принадлежит текущей
     * организации. Для чужой/удалённой/отсутствующей сборки каталог бросает
     * исключение «не найдено».
     */
    private void requireAssembly(AssemblyId assemblyId) {
        assemblyService.get(assemblyId);
    }
}
