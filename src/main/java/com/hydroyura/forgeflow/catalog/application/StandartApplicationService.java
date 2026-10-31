package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.CreateStandartRequest;
import com.hydroyura.forgeflow.catalog.api.StandartFilter;
import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.api.StandartService;
import com.hydroyura.forgeflow.catalog.api.StandartView;
import com.hydroyura.forgeflow.catalog.domain.standart.Standart;
import com.hydroyura.forgeflow.catalog.domain.standart.StandartRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Реализация {@link StandartService}: CRUD-срез стандартных изделий каталога.
 */
public class StandartApplicationService implements StandartService {

    private final StandartRepository repository;
    private final OrganizationContext organizationContext;
    private final StandartMapper mapper;
    private final Clock clock;

    public StandartApplicationService(
            StandartRepository repository,
            OrganizationContext organizationContext,
            StandartMapper mapper,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public StandartView create(CreateStandartRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (repository.existsByNumber(request.number(), organizationId)) {
            throw new DuplicateStandartNumberException(request.number());
        }
        Instant now = clock.instant();
        Standart standart = Standart.create(StandartId.generate(), request.number(), request.name(), organizationId, now, now);
        repository.save(standart);
        return mapper.toView(standart);
    }

    @Override
    public StandartView get(StandartId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Standart standart = repository.findById(id, organizationId)
                .orElseThrow(() -> new StandartNotFoundException(id));
        return mapper.toView(standart);
    }

    @Override
    public List<StandartView> list(StandartFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.findAll(organizationId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public void rename(StandartId id, String newName) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Standart standart = repository.findById(id, organizationId)
                .orElseThrow(() -> new StandartNotFoundException(id));
        standart.rename(newName);
        repository.save(standart);
    }

    @Override
    public void delete(StandartId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Standart standart = repository.findById(id, organizationId)
                .orElseThrow(() -> new StandartNotFoundException(id));
        standart.markDeleted();
        repository.save(standart);
    }
}
