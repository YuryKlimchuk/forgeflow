package com.hydroyura.forgeflow.technology.application;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.catalog.api.PartService;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.AddOperationRequest;
import com.hydroyura.forgeflow.technology.api.CreateTechnologyProcessRequest;
import com.hydroyura.forgeflow.technology.api.OperationView;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessView;
import com.hydroyura.forgeflow.technology.api.TechnologyService;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessNotFoundException;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessRepository;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@link TechnologyService}: управление технологическими процессами.
 * Кросс-агрегатные правила делегируются в доменный {@link TechnologyProcessService},
 * текущая организация берётся из {@link OrganizationContext}.
 */
@Transactional
public class TechnologyApplicationService implements TechnologyService {

    private final TechnologyProcessRepository repository;
    private final TechnologyProcessService domainService;
    private final OrganizationContext organizationContext;
    private final PartService partService;
    private final TechnologyMapper mapper;
    private final Clock clock;

    public TechnologyApplicationService(
            TechnologyProcessRepository repository,
            TechnologyProcessService domainService,
            OrganizationContext organizationContext,
            PartService partService,
            TechnologyMapper mapper,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.domainService = Objects.requireNonNull(domainService, "domainService must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.partService = Objects.requireNonNull(partService, "partService must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public TechnologyProcessView create(CreateTechnologyProcessRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (Objects.isNull(request.partId())) {
            throw new IllegalArgumentException("partId must not be null");
        }
        Instant now = clock.instant();
        TechnologyProcess process = TechnologyProcess.create(
                TechnologyProcessId.generate(),
                request.number(),
                request.name(),
                request.partId(),
                organizationId,
                now,
                now);
        partService.get(request.partId());
        domainService.register(process);
        return mapper.toView(process);
    }

    @Override
    public TechnologyProcessView get(TechnologyProcessId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        TechnologyProcess process = repository.findById(id, organizationId)
                .orElseThrow(() -> new TechnologyProcessNotFoundException(id));
        return mapper.toView(process);
    }

    @Override
    public List<TechnologyProcessView> list(PartId partId) {
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        List<TechnologyProcess> processes = Objects.isNull(partId)
                ? repository.findAll(organizationId)
                : repository.findByPartId(partId, organizationId);
        return processes.stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public void rename(TechnologyProcessId id, String newName) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        TechnologyProcess process = repository.findById(id, organizationId)
                .orElseThrow(() -> new TechnologyProcessNotFoundException(id));
        process.rename(newName);
        repository.save(process);
    }

    @Override
    public void delete(TechnologyProcessId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        TechnologyProcess process = repository.findById(id, organizationId)
                .orElseThrow(() -> new TechnologyProcessNotFoundException(id));
        process.markDeleted();
        repository.save(process);
    }

    @Override
    public OperationView addOperation(TechnologyProcessId id, AddOperationRequest request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        TechnologyProcess process = repository.findById(id, organizationId)
                .orElseThrow(() -> new TechnologyProcessNotFoundException(id));
        process.addOperation(request.number(), request.name());
        repository.save(process);
        return new OperationView(request.number(), request.name());
    }

    @Override
    public void removeOperation(TechnologyProcessId id, int number) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        TechnologyProcess process = repository.findById(id, organizationId)
                .orElseThrow(() -> new TechnologyProcessNotFoundException(id));
        process.removeOperation(number);
        repository.save(process);
    }

    @Override
    public void markAsDefault(TechnologyProcessId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        domainService.markAsDefault(id, organizationId);
    }

    @Override
    public void unmarkDefault(TechnologyProcessId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        domainService.unmarkDefault(id, organizationId);
    }
}
