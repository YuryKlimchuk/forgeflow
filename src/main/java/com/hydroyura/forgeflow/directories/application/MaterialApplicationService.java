package com.hydroyura.forgeflow.directories.application;

import com.hydroyura.forgeflow.directories.api.CreateMaterialRequest;
import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.directories.api.MaterialService;
import com.hydroyura.forgeflow.directories.api.MaterialView;
import com.hydroyura.forgeflow.directories.api.UpdateMaterialRequest;
import com.hydroyura.forgeflow.directories.domain.material.DuplicateMaterialNumberException;
import com.hydroyura.forgeflow.directories.domain.material.Material;
import com.hydroyura.forgeflow.directories.domain.material.MaterialNotFoundException;
import com.hydroyura.forgeflow.directories.domain.material.MaterialRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@link MaterialService}: управление материалами.
 *
 * <p>Текущая организация берётся из {@link OrganizationContext} и передаётся в
 * репозиторий явным аргументом; репозиторий контекст не читает.</p>
 */
@Transactional
public class MaterialApplicationService implements MaterialService {

    private final MaterialRepository repository;
    private final OrganizationContext organizationContext;
    private final MaterialMapper mapper;
    private final Clock clock;

    public MaterialApplicationService(
            MaterialRepository repository,
            OrganizationContext organizationContext,
            MaterialMapper mapper,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public MaterialView create(CreateMaterialRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (repository.existsByNumber(request.number(), organizationId)) {
            throw new DuplicateMaterialNumberException(request.number());
        }
        Instant now = clock.instant();
        Material material = Material.create(
                MaterialId.generate(),
                request.number(),
                request.name(),
                request.standardType(),
                request.standard(),
                request.unit(),
                organizationId,
                now,
                now);
        repository.save(material);
        return mapper.toView(material);
    }

    @Override
    public MaterialView get(MaterialId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Material material = repository.findById(id, organizationId)
                .orElseThrow(() -> new MaterialNotFoundException(id));
        return mapper.toView(material);
    }

    @Override
    public List<MaterialView> list() {
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.findAll(organizationId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public void update(MaterialId id, UpdateMaterialRequest request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Material material = repository.findById(id, organizationId)
                .orElseThrow(() -> new MaterialNotFoundException(id));
        material.update(request.name(), request.standardType(), request.standard(), request.unit());
        repository.save(material);
    }

    @Override
    public void delete(MaterialId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Material material = repository.findById(id, organizationId)
                .orElseThrow(() -> new MaterialNotFoundException(id));
        material.markDeleted();
        repository.save(material);
    }
}
