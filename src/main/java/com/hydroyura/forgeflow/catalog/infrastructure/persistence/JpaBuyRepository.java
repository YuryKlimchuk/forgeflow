package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.catalog.domain.buy.Buy;
import com.hydroyura.forgeflow.catalog.domain.buy.BuyRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JpaBuyRepository implements BuyRepository {

    private final BuyJpaRepository repository;
    private final BuyMapper mapper = BuyMapper.INSTANCE;

    public JpaBuyRepository(BuyJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Buy buy) {
        repository.save(mapper.toEntity(buy));
    }

    @Override
    public Optional<Buy> findById(BuyId id, OrganizationId organizationId) {
        return repository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId.value(), id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Buy> findAll(OrganizationId organizationId) {
        return repository.findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(organizationId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return repository.existsByOrganizationIdAndNumberAndDeletedAtIsNull(organizationId.value(), number);
    }
}
