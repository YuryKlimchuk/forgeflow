package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecificationRowJpaRepository extends JpaRepository<SpecificationRowEntity, SpecificationRowId> {

    List<SpecificationRowEntity> findByOrganizationIdAndAssemblyIdOrderByItemTypeAscItemIdAsc(
            UUID organizationId, UUID assemblyId);

    void deleteByOrganizationIdAndAssemblyId(UUID organizationId, UUID assemblyId);
}
