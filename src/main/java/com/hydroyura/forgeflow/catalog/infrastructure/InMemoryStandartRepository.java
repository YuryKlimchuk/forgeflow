package com.hydroyura.forgeflow.catalog.infrastructure;

import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.domain.standart.Standart;
import com.hydroyura.forgeflow.catalog.domain.standart.StandartRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link StandartRepository}.
 */
public class InMemoryStandartRepository implements StandartRepository {

    private final Map<StandartId, Standart> standarts = new ConcurrentHashMap<>();

    @Override
    public void save(Standart standart) {
        standarts.put(standart.getId(), standart);
    }

    @Override
    public Optional<Standart> findById(StandartId id, OrganizationId organizationId) {
        Standart standart = standarts.get(id);
        if (standart == null || !standart.getOrganizationId().equals(organizationId) || standart.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(standart);
    }

    @Override
    public List<Standart> findAll(OrganizationId organizationId) {
        return standarts.values().stream()
                .filter(standart -> standart.getOrganizationId().equals(organizationId))
                .filter(standart -> standart.getDeletedAt() == null)
                .sorted(Comparator.comparing(Standart::getNumber))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return standarts.values().stream()
                .anyMatch(standart -> standart.getOrganizationId().equals(organizationId)
                        && standart.getNumber().equals(number)
                        && standart.getDeletedAt() == null);
    }
}
