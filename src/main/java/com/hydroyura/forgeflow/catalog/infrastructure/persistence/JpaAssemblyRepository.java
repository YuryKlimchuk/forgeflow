package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.catalog.domain.assembly.Assembly;
import com.hydroyura.forgeflow.catalog.domain.assembly.AssemblyRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JpaAssemblyRepository implements AssemblyRepository {

    private final AssemblyJpaRepository repository;
    private final AssemblyMapper mapper = AssemblyMapper.INSTANCE;

    public JpaAssemblyRepository(AssemblyJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Assembly assembly) {
        repository.save(mapper.toEntity(assembly));
    }

    @Override
    public Optional<Assembly> findById(AssemblyId id, OrganizationId organizationId) {
        return repository.findByOrganizationIdAndIdAndDeletedAtIsNull(organizationId.value(), id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Assembly> findAll(OrganizationId organizationId) {
        return repository.findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(organizationId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNumber(String number, OrganizationId organizationId) {
        return repository.existsByOrganizationIdAndNumberAndDeletedAtIsNull(organizationId.value(), number);
    }
}
