package com.hydroyura.forgeflow.planning.application;

import com.hydroyura.forgeflow.planning.api.PlanItemView;
import com.hydroyura.forgeflow.planning.api.PlanView;
import com.hydroyura.forgeflow.planning.domain.Plan;
import java.util.List;

/**
 * Маппер плана в публичное представление.
 */
public class PlanMapper {

    public PlanView toView(Plan plan) {
        List<PlanItemView> items = plan.items().stream()
                .map(item -> new PlanItemView(item.assemblyId(), item.quantity()))
                .toList();
        return new PlanView(plan.getId(), plan.getName(), plan.getYear(), plan.getMonth(), items);
    }
}
