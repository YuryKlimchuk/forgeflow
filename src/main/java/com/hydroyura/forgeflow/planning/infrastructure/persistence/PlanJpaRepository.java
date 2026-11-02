package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanJpaRepository extends JpaRepository<PlanEntity, UUID> {

    List<PlanEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<PlanEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

    boolean existsByOrganizationIdAndName(UUID organizationId, String name);
}
