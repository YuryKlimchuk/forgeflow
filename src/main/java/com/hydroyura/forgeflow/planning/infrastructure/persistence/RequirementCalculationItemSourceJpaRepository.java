package com.hydroyura.forgeflow.planning.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequirementCalculationItemSourceJpaRepository
        extends JpaRepository<RequirementCalculationItemSourceEntity, RequirementCalculationItemSourceId> {

    List<RequirementCalculationItemSourceEntity> findByCalculationItemIdIn(Collection<java.util.UUID> calculationItemIds);
}
