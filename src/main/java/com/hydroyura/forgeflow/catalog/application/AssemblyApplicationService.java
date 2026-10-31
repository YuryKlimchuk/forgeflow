package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.AssemblyFilter;
import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.api.AssemblyService;
import com.hydroyura.forgeflow.catalog.api.AssemblyView;
import com.hydroyura.forgeflow.catalog.api.CreateAssemblyRequest;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Реализация {@link AssemblyService}: CRUD-срез сборочных единиц каталога.
 */
public class AssemblyApplicationService implements AssemblyService {

    private final AssemblyRepository repository;
    private final OrganizationContext organizationContext;
    private final AssemblyMapper mapper;
    private final Clock clock;

    public AssemblyApplicationService(
            AssemblyRepository repository,
            OrganizationContext organizationContext,
            AssemblyMapper mapper,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public AssemblyView create(CreateAssemblyRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (repository.existsByNumber(request.number(), organizationId)) {
            throw new DuplicateAssemblyNumberException(request.number());
        }
        Instant now = clock.instant();
        Assembly assembly = Assembly.create(AssemblyId.generate(), request.number(), request.name(), organizationId, now, now);
        repository.save(assembly);
        return mapper.toView(assembly);
    }

    @Override
    public AssemblyView get(AssemblyId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Assembly assembly = repository.findById(id, organizationId)
                .orElseThrow(() -> new AssemblyNotFoundException(id));
        return mapper.toView(assembly);
    }

    @Override
    public List<AssemblyView> list(AssemblyFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.findAll(organizationId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public void rename(AssemblyId id, String newName) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Assembly assembly = repository.findById(id, organizationId)
                .orElseThrow(() -> new AssemblyNotFoundException(id));
        assembly.rename(newName);
        repository.save(assembly);
    }

    @Override
    public void delete(AssemblyId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Assembly assembly = repository.findById(id, organizationId)
                .orElseThrow(() -> new AssemblyNotFoundException(id));
        assembly.markDeleted();
        repository.save(assembly);
    }
}
