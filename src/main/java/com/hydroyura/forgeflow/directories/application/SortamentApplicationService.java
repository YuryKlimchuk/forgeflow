package com.hydroyura.forgeflow.directories.application;

import com.hydroyura.forgeflow.directories.api.CreateSortamentRequest;
import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.api.SortamentService;
import com.hydroyura.forgeflow.directories.api.SortamentView;
import com.hydroyura.forgeflow.directories.api.UpdateSortamentRequest;
import com.hydroyura.forgeflow.directories.domain.sortament.DuplicateSortamentNumberException;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentNotFoundException;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@link SortamentService}: управление сортаментами.
 *
 * <p>Текущая организация берётся из {@link OrganizationContext} и передаётся в
 * репозиторий явным аргументом; репозиторий контекст не читает.</p>
 */
@Transactional
public class SortamentApplicationService implements SortamentService {

    private final SortamentRepository repository;
    private final OrganizationContext organizationContext;
    private final SortamentMapper mapper;
    private final Clock clock;

    public SortamentApplicationService(
            SortamentRepository repository,
            OrganizationContext organizationContext,
            SortamentMapper mapper,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public SortamentView create(CreateSortamentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        if (repository.existsByNumber(request.number(), organizationId)) {
            throw new DuplicateSortamentNumberException(request.number());
        }
        Instant now = clock.instant();
        Sortament sortament = Sortament.create(
                SortamentId.generate(),
                request.number(),
                request.name(),
                request.standardType(),
                request.standard(),
                request.unit(),
                organizationId,
                now,
                now);
        repository.save(sortament);
        return mapper.toView(sortament);
    }

    @Override
    public SortamentView get(SortamentId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Sortament sortament = repository.findById(id, organizationId)
                .orElseThrow(() -> new SortamentNotFoundException(id));
        return mapper.toView(sortament);
    }

    @Override
    public List<SortamentView> list() {
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.findAll(organizationId).stream()
                .map(mapper::toView)
                .toList();
    }

    @Override
    public void update(SortamentId id, UpdateSortamentRequest request) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Sortament sortament = repository.findById(id, organizationId)
                .orElseThrow(() -> new SortamentNotFoundException(id));
        sortament.update(request.name(), request.standardType(), request.standard(), request.unit());
        repository.save(sortament);
    }

    @Override
    public void delete(SortamentId id) {
        Objects.requireNonNull(id, "id must not be null");
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        Sortament sortament = repository.findById(id, organizationId)
                .orElseThrow(() -> new SortamentNotFoundException(id));
        sortament.markDeleted();
        repository.save(sortament);
    }
}
