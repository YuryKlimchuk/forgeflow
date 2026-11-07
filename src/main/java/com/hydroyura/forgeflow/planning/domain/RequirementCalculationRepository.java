package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения расчётов потребностей. Все операции принимают
 * {@link OrganizationId} и {@link PlanId} явными аргументами.
 */
public interface RequirementCalculationRepository {

    void save(RequirementCalculation calculation);

    List<RequirementCalculation> findByPlanId(OrganizationId organizationId, PlanId planId);

    Optional<RequirementCalculation> findById(
            OrganizationId organizationId,
            PlanId planId,
            RequirementCalculationId calculationId);
}
