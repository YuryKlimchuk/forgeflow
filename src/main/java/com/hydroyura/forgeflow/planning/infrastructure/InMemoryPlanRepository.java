package com.hydroyura.forgeflow.planning.infrastructure;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.planning.domain.Plan;
import com.hydroyura.forgeflow.planning.domain.PlanRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link PlanRepository}: планы изолированы по организации.
 */
public class InMemoryPlanRepository implements PlanRepository {

    private final Map<OrganizationId, Map<PlanId, Plan>> byOrganization = new ConcurrentHashMap<>();

    @Override
    public void save(Plan plan) {
        byOrganization.computeIfAbsent(plan.getOrganizationId(), ignored -> new ConcurrentHashMap<>())
                .put(plan.getId(), plan);
    }

    @Override
    public Optional<Plan> findById(OrganizationId organizationId, PlanId id) {
        Map<PlanId, Plan> plans = byOrganization.get(organizationId);
        return Optional.ofNullable(plans == null ? null : plans.get(id));
    }

    @Override
    public List<Plan> findAll(OrganizationId organizationId) {
        Map<PlanId, Plan> plans = byOrganization.get(organizationId);
        if (plans == null) {
            return List.of();
        }
        List<Plan> result = new ArrayList<>(plans.values());
        result.sort(Comparator.comparing(Plan::getName));
        return result;
    }

    @Override
    public boolean existsByName(OrganizationId organizationId, String name) {
        Map<PlanId, Plan> plans = byOrganization.get(organizationId);
        return plans != null && plans.values().stream().anyMatch(plan -> plan.getName().equals(name));
    }
}
