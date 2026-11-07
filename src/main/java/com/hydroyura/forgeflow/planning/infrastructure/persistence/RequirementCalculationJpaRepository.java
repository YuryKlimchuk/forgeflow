package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementCalculationJpaRepository extends JpaRepository<RequirementCalculationEntity, UUID> {

    List<RequirementCalculationEntity> findByOrganizationIdAndPlanIdOrderByExecutedAtDesc(
            UUID organizationId, UUID planId);

    Optional<RequirementCalculationEntity> findByOrganizationIdAndPlanIdAndId(
            UUID organizationId, UUID planId, UUID id);
}
