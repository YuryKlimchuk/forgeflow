package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.CreateVzkRequest;
import com.hydroyura.forgeflow.catalog.api.VzkFilter;
import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.api.VzkService;
import com.hydroyura.forgeflow.catalog.api.VzkView;
import com.hydroyura.forgeflow.catalog.domain.vzk.Vzk;
import com.hydroyura.forgeflow.catalog.domain.vzk.VzkRepository;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettingsProvider;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Реализация {@link VzkService}: CRUD-срез групп ВЗК каталога.
 */
public class VzkApplicationService implements VzkService {

    private final VzkRepository repository;
    private final OrganizationContext organizationContext;
    private final OrganizationSettingsProvider settingsProvider;
    private final VzkMapper mapper;
    private final Clock clock;

    public VzkApplicationService(
            VzkRepository repository,
            OrganizationContext organizationContext,
            OrganizationSettingsProvider settingsProvider,
            VzkMapper mapper,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public VzkView create(CreateVzkRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (!settingsProvider.findSettings(organizationId).matchesVzk(request.number())) {
            throw new IllegalArgumentException("Vzk number does not match the organization pattern: " + request.number());
        }
        if (repository.existsByNumber(request.number(), organizationId)) {
            throw new DuplicateVzkNumberException(request.number());
        }
        Instant now = clock.instant();
        Vzk vzk = Vzk.create(VzkId.generate(), request.number(), request.name(), organizationId, now, now);
        repository.save(vzk);
        return mapper.toView(vzk);
    }

    @Override
    public VzkView get(VzkId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Vzk vzk = repository.findById(id, organizationId)
                .orElseThrow(() -> new VzkNotFoundException(id));
        return mapper.toView(vzk);
    }

    @Override
    public List<VzkView> list(VzkFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.findAll(organizationId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public void rename(VzkId id, String newName) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Vzk vzk = repository.findById(id, organizationId)
                .orElseThrow(() -> new VzkNotFoundException(id));
        vzk.rename(newName);
        repository.save(vzk);
    }

    @Override
    public void delete(VzkId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Vzk vzk = repository.findById(id, organizationId)
                .orElseThrow(() -> new VzkNotFoundException(id));
        vzk.markDeleted();
        repository.save(vzk);
    }
}
