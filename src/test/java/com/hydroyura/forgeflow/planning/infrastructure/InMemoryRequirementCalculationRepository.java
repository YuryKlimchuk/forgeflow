package com.hydroyura.forgeflow.planning.infrastructure;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculation;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculationRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link RequirementCalculationRepository} с изоляцией по
 * организации и плану.
 */
public class InMemoryRequirementCalculationRepository implements RequirementCalculationRepository {

    private final Map<OrganizationId, Map<PlanId, Map<RequirementCalculationId, RequirementCalculation>>> byOrganization =
            new ConcurrentHashMap<>();

    @Override
    public void save(RequirementCalculation calculation) {
        byOrganization
                .computeIfAbsent(calculation.getOrganizationId(), ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(calculation.getPlanId(), ignored -> new ConcurrentHashMap<>())
                .put(calculation.getId(), calculation);
    }

    @Override
    public List<RequirementCalculation> findByPlanId(OrganizationId organizationId, PlanId planId) {
        Map<PlanId, Map<RequirementCalculationId, RequirementCalculation>> plans = byOrganization.get(organizationId);
        if (plans == null) {
            return List.of();
        }
        Map<RequirementCalculationId, RequirementCalculation> calculations = plans.get(planId);
        if (calculations == null) {
            return List.of();
        }
        List<RequirementCalculation> result = new ArrayList<>(calculations.values());
        result.sort(Comparator
                .comparing(RequirementCalculation::getExecutedAt)
                .reversed()
                .thenComparing(calc -> calc.getId().value()));
        return result;
    }

    @Override
    public Optional<RequirementCalculation> findById(
            OrganizationId organizationId,
            PlanId planId,
            RequirementCalculationId calculationId) {
        Map<PlanId, Map<RequirementCalculationId, RequirementCalculation>> plans = byOrganization.get(organizationId);
        if (plans == null) {
            return Optional.empty();
        }
        Map<RequirementCalculationId, RequirementCalculation> calculations = plans.get(planId);
        return Optional.ofNullable(calculations == null ? null : calculations.get(calculationId));
    }
}
