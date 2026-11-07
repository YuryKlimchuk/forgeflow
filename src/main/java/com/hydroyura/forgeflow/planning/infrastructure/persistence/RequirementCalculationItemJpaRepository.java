package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementCalculationItemJpaRepository
        extends JpaRepository<RequirementCalculationItemEntity, UUID> {

    List<RequirementCalculationItemEntity> findByCalculationId(UUID calculationId);
}
