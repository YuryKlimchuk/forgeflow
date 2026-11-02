package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanItemJpaRepository extends JpaRepository<PlanItemEntity, PlanItemId> {

    List<PlanItemEntity> findByPlanIdOrderByAssemblyIdAsc(UUID planId);

    void deleteByPlanId(UUID planId);
}
