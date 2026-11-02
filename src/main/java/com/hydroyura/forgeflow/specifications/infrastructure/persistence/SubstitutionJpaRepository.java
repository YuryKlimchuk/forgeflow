package com.hydroyura.forgeflow.specifications.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubstitutionJpaRepository extends JpaRepository<SubstitutionEntity, SubstitutionId> {

    List<SubstitutionEntity> findByOrganizationIdAndAssemblyIdAndItemTypeAndItemId(
            UUID organizationId, UUID assemblyId, String itemType, UUID itemId);

    void deleteByOrganizationIdAndAssemblyIdAndItemTypeAndItemId(
            UUID organizationId, UUID assemblyId, String itemType, UUID itemId);
}
