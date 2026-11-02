package com.hydroyura.forgeflow.catalog.infrastructure;

import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.domain.buy.Buy;
import com.hydroyura.forgeflow.catalog.domain.buy.BuyRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link BuyRepository}.
 */
public class InMemoryBuyRepository implements BuyRepository {

    private final Map<BuyId, Buy> buys = new ConcurrentHashMap<>();

    @Override
    public void save(Buy buy) {
        buys.put(buy.getId(), buy);
    }

    @Override
    public Optional<Buy> findById(BuyId id, OrganizationId organizationId) {
        Buy buy = buys.get(id);
        if (buy == null || !buy.getOrganizationId().equals(organizationId) || buy.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(buy);
    }

    @Override
    public List<Buy> findAll(OrganizationId organizationId) {
        return buys.values().stream()
                .filter(buy -> buy.getOrganizationId().equals(organizationId))
                .filter(buy -> buy.getDeletedAt() == null)
                .sorted(Comparator.comparing(Buy::getNumber))
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return buys.values().stream()
                .anyMatch(buy -> buy.getOrganizationId().equals(organizationId)
                        && buy.getNumber().equals(number)
                        && buy.getDeletedAt() == null);
    }
}
