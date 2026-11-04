package com.hydroyura.forgeflow.technology.domain;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import java.util.List;
import java.util.Objects;

/**
 * Доменный сервис, принуждающий кросс-агрегатные правила технологических процессов:
 * уникальность номера в организации и инвариант дефолтного процесса на деталь.
 */
public class TechnologyProcessService {

    private final TechnologyProcessRepository repository;

    public TechnologyProcessService(TechnologyProcessRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public void register(TechnologyProcess process) {
        Objects.requireNonNull(process, "process must not be null");
        OrganizationId organizationId = process.getOrganizationId();
        if (repository.existsByNumber(process.getNumber(), organizationId)) {
            throw new DuplicateTechnologyProcessNumberException(process.getNumber());
        }
        if (!process.isDefaultProcess()
                && isSecondNonDefaultProcess(process.getPartId(), organizationId)) {
            throw new InvalidDefaultProcessStateException(process.getPartId());
        }
        repository.save(process);
    }

    public void markAsDefault(TechnologyProcessId id, OrganizationId organizationId) {
        TechnologyProcess process = requireProcess(id, organizationId);
        if (process.isDefaultProcess()) {
            return;
        }
        repository.findByPartId(process.getPartId(), organizationId).stream()
                .filter(other -> !other.getId().equals(process.getId()))
                .filter(TechnologyProcess::isDefaultProcess)
                .forEach(other -> {
                    other.markAsNotDefault();
                    repository.save(other);
                });
        process.markAsDefault();
        repository.save(process);
    }

    public void unmarkDefault(TechnologyProcessId id, OrganizationId organizationId) {
        TechnologyProcess process = requireProcess(id, organizationId);
        if (!process.isDefaultProcess()) {
            return;
        }
        boolean hasAnotherActiveProcess = repository.findByPartId(process.getPartId(), organizationId).stream()
                .anyMatch(other -> !other.getId().equals(process.getId()));
        if (hasAnotherActiveProcess) {
            throw new InvalidDefaultProcessStateException(process.getPartId());
        }
        process.markAsNotDefault();
        repository.save(process);
    }

    private TechnologyProcess requireProcess(TechnologyProcessId id, OrganizationId organizationId) {
        return repository.findById(id, organizationId)
                .orElseThrow(() -> new TechnologyProcessNotFoundException(id));
    }

    private boolean isSecondNonDefaultProcess(PartId partId, OrganizationId organizationId) {
        List<TechnologyProcess> partProcesses = repository.findByPartId(partId, organizationId);
        return !partProcesses.isEmpty()
                && partProcesses.stream().noneMatch(TechnologyProcess::isDefaultProcess);
    }
}
