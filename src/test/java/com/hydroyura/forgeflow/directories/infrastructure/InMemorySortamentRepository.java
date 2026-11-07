package com.hydroyura.forgeflow.directories.infrastructure;

import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.directories.domain.sortament.Sortament;
import com.hydroyura.forgeflow.directories.domain.sortament.SortamentRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link SortamentRepository} для тестов.
 */
public class InMemorySortamentRepository implements SortamentRepository {

    private final Map<SortamentId, Sortament> sortaments = new ConcurrentHashMap<>();

    @Override
    public void save(Sortament sortament) {
        sortaments.put(sortament.getId(), sortament);
    }

    @Override
    public Optional<Sortament> findById(SortamentId id, OrganizationId organizationId) {
        Sortament sortament = sortaments.get(id);
        if (sortament == null || !sortament.getOrganizationId().equals(organizationId) || sortament.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(sortament);
    }

    @Override
    public List<Sortament> findAll(OrganizationId organizationId) {
        return sortaments.values().stream()
                .filter(sortament -> sortament.getOrganizationId().equals(organizationId))
                .filter(sortament -> sortament.getDeletedAt() == null)
                .sorted(Comparator.comparing(Sortament::getNumber))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return sortaments.values().stream()
                .anyMatch(sortament -> sortament.getOrganizationId().equals(organizationId)
                        && sortament.getNumber().equals(number)
                        && sortament.getDeletedAt() == null);
    }
}
