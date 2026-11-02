package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.specifications.domain.Substitution;
import com.hydroyura.forgeflow.specifications.domain.SubstitutionRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JpaSubstitutionRepository implements SubstitutionRepository {

    private final SubstitutionJpaRepository repository;
    private final SubstitutionMapper mapper = SubstitutionMapper.INSTANCE;

    public JpaSubstitutionRepository(SubstitutionJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Substitution substitution) {
        UUID organizationId = substitution.organizationId().value();
        UUID assemblyId = substitution.assemblyId().value();
        String itemType = substitution.item().type().name();
        UUID itemId = substitution.item().id();
        repository.deleteByOrganizationIdAndAssemblyIdAndItemTypeAndItemId(
                organizationId, assemblyId, itemType, itemId);
        repository.flush();
        repository.saveAll(mapper.toEntities(substitution));
    }

    @Override
    public Optional<Substitution> findById(
            OrganizationId organizationId, AssemblyId assemblyId, CatalogItemReference item) {
        List<SubstitutionEntity> rows =
                repository.findByOrganizationIdAndAssemblyIdAndItemTypeAndItemId(
                        organizationId.value(), assemblyId.value(), item.type().name(), item.id());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(organizationId, assemblyId, item, rows));
    }

    @Override
    public void delete(OrganizationId organizationId, AssemblyId assemblyId, CatalogItemReference item) {
        repository.deleteByOrganizationIdAndAssemblyIdAndItemTypeAndItemId(
                organizationId.value(), assemblyId.value(), item.type().name(), item.id());
    }
}
