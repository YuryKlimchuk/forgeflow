package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.domain.Specification;
import com.hydroyura.forgeflow.specifications.domain.SpecificationRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JpaSpecificationRepository implements SpecificationRepository {

    private final SpecificationRowJpaRepository repository;
    private final SpecificationMapper mapper = SpecificationMapper.INSTANCE;

    public JpaSpecificationRepository(SpecificationRowJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Specification specification) {
        UUID organizationId = specification.organizationId().value();
        UUID assemblyId = specification.assemblyId().value();
        repository.deleteByOrganizationIdAndAssemblyId(organizationId, assemblyId);
        repository.flush();
        repository.saveAll(mapper.toEntities(specification));
    }

    @Override
    public Optional<Specification> findById(OrganizationId organizationId, AssemblyId assemblyId) {
        List<SpecificationRowEntity> rows =
                repository.findByOrganizationIdAndAssemblyIdOrderByItemTypeAscItemIdAsc(
                        organizationId.value(), assemblyId.value());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(organizationId, assemblyId, rows));
    }
}
