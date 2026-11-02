package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.catalog.domain.standart.Standart;
import com.hydroyura.forgeflow.catalog.domain.standart.StandartRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JpaStandartRepository implements StandartRepository {

    private final StandartJpaRepository repository;
    private final StandartMapper mapper = StandartMapper.INSTANCE;

    public JpaStandartRepository(StandartJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Standart standart) {
        repository.save(mapper.toEntity(standart));
    }

    @Override
    public Optional<Standart> findById(StandartId id, OrganizationId organizationId) {
        return repository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId.value(), id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Standart> findAll(OrganizationId organizationId) {
        return repository.findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(organizationId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return repository.existsByOrganizationIdAndNumberAndDeletedAtIsNull(organizationId.value(), number);
    }
}
