package com.hydroyura.forgeflow.technology.infrastructure;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcess;
import com.hydroyura.forgeflow.technology.domain.TechnologyProcessRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link TechnologyProcessRepository} для тестов.
 *
 * <p>Хранит процессы по {@link TechnologyProcessId}; чтение фильтрует по
 * организации и исключает удалённые записи; списки сортируются по номеру.</p>
 */
public class InMemoryTechnologyProcessRepository implements TechnologyProcessRepository {

    private final Map<TechnologyProcessId, TechnologyProcess> processes = new ConcurrentHashMap<>();

    @Override
    public void save(TechnologyProcess process) {
        processes.put(process.getId(), process);
    }

    @Override
    public Optional<TechnologyProcess> findById(TechnologyProcessId id, OrganizationId organizationId) {
        TechnologyProcess process = processes.get(id);
        if (process == null || !process.getOrganizationId().equals(organizationId) || process.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(process);
    }

    @Override
    public List<TechnologyProcess> findByPartId(PartId partId, OrganizationId organizationId) {
        return processes.values().stream()
                .filter(process -> process.getOrganizationId().equals(organizationId))
                .filter(process -> process.getPartId().equals(partId))
                .filter(process -> process.getDeletedAt() == null)
                .sorted(Comparator.comparing(TechnologyProcess::getNumber))
                .toList();
    }

    @Override
    public List<TechnologyProcess> findAll(OrganizationId organizationId) {
        return processes.values().stream()
                .filter(process -> process.getOrganizationId().equals(organizationId))
                .filter(process -> process.getDeletedAt() == null)
                .sorted(Comparator.comparing(TechnologyProcess::getNumber))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return processes.values().stream()
                .anyMatch(process -> process.getOrganizationId().equals(organizationId)
                        && process.getNumber().equals(number)
                        && process.getDeletedAt() == null);
    }
}
