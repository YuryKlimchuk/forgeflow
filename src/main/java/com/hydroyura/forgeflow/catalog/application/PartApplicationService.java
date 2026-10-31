package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.CreatePartRequest;
import com.hydroyura.forgeflow.catalog.api.PartFilter;
import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.catalog.api.PartView;
import com.hydroyura.forgeflow.catalog.domain.part.Part;
import com.hydroyura.forgeflow.catalog.domain.part.PartRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Реализация {@link PartService}: эталонный CRUD-срез деталей каталога.
 *
 * <p>Текущая организация берётся из {@link OrganizationContext} и передаётся в
 * репозиторий явным аргументом; репозиторий контекст не читает.</p>
 */
public class PartApplicationService implements PartService {

    private final PartRepository repository;
    private final OrganizationContext organizationContext;
    private final PartMapper mapper;
    private final Clock clock;

    public PartApplicationService(
            PartRepository repository,
            OrganizationContext organizationContext,
            PartMapper mapper,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public PartView create(CreatePartRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (repository.existsByNumber(request.number(), organizationId)) {
            throw new DuplicatePartNumberException(request.number());
        }
        Instant now = clock.instant();
        Part part = Part.create(PartId.generate(), request.number(), request.name(), organizationId, now, now);
        repository.save(part);
        return mapper.toView(part);
    }

    @Override
    public PartView get(PartId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Part part = repository.findById(id, organizationId)
                .orElseThrow(() -> new PartNotFoundException(id));
        return mapper.toView(part);
    }

    @Override
    public List<PartView> list(PartFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.findAll(organizationId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public void rename(PartId id, String newName) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Part part = repository.findById(id, organizationId)
                .orElseThrow(() -> new PartNotFoundException(id));
        part.rename(newName);
        repository.save(part);
    }

    @Override
    public void delete(PartId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Part part = repository.findById(id, organizationId)
                .orElseThrow(() -> new PartNotFoundException(id));
        part.markDeleted();
        repository.save(part);
    }
}
