package com.hydroyura.forgeflow.catalog.infrastructure;

import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.catalog.domain.vzk.Vzk;
import com.hydroyura.forgeflow.catalog.domain.vzk.VzkRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link VzkRepository}.
 */
public class InMemoryVzkRepository implements VzkRepository {

    private final Map<VzkId, Vzk> vzks = new ConcurrentHashMap<>();

    @Override
    public void save(Vzk vzk) {
        vzks.put(vzk.getId(), vzk);
    }

    @Override
    public Optional<Vzk> findById(VzkId id, OrganizationId organizationId) {
        Vzk vzk = vzks.get(id);
        if (vzk == null || !vzk.getOrganizationId().equals(organizationId) || vzk.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(vzk);
    }

    @Override
    public List<Vzk> findAll(OrganizationId organizationId) {
        return vzks.values().stream()
                .filter(vzk -> vzk.getOrganizationId().equals(organizationId))
                .filter(vzk -> vzk.getDeletedAt() == null)
                .sorted(Comparator.comparing(Vzk::getNumber))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return vzks.values().stream()
                .anyMatch(vzk -> vzk.getOrganizationId().equals(organizationId)
                        && vzk.getNumber().equals(number)
                        && vzk.getDeletedAt() == null);
    }
}
