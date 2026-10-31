package com.forgeflow.catalog.infrastructure;

import com.forgeflow.catalog.api.PartId;
import com.forgeflow.catalog.domain.part.Part;
import com.forgeflow.catalog.domain.part.PartRepository;
import com.forgeflow.shared.api.OrganizationId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link PartRepository} для эталонного среза каталога.
 *
 * <p>Хранит детали по {@link PartId}; чтение фильтрует по организации и
 * исключает удалённые записи; список сортируется по номеру.</p>
 */
public class InMemoryPartRepository implements PartRepository {

    private final Map<PartId, Part> parts = new ConcurrentHashMap<>();

    @Override
    public void save(Part part) {
        parts.put(part.getId(), part);
    }

    @Override
    public Optional<Part> findById(PartId id, OrganizationId organizationId) {
        Part part = parts.get(id);
        if (part == null || !part.getOrganizationId().equals(organizationId) || part.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(part);
    }

    @Override
    public List<Part> findAll(OrganizationId organizationId) {
        return parts.values().stream()
                .filter(part -> part.getOrganizationId().equals(organizationId))
                .filter(part -> part.getDeletedAt() == null)
                .sorted(Comparator.comparing(Part::getNumber))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return parts.values().stream()
                .anyMatch(part -> part.getOrganizationId().equals(organizationId)
                        && part.getNumber().equals(number)
                        && part.getDeletedAt() == null);
    }
}
