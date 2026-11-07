package com.hydroyura.forgeflow.planning.application;

import com.hydroyura.forgeflow.planning.api.RequirementCalculationView;
import com.hydroyura.forgeflow.planning.api.RequirementItemView;
import com.hydroyura.forgeflow.planning.domain.RequirementCalculation;
import com.hydroyura.forgeflow.planning.domain.RequirementItem;
import java.util.List;

/**
 * Маппер расчёта потребностей в публичное представление.
 */
public class RequirementCalculationMapper {

    public RequirementCalculationView toView(RequirementCalculation calculation) {
        List<RequirementItemView> items = calculation.items().stream()
                .map(this::toItemView)
                .toList();
        return new RequirementCalculationView(
                calculation.getId(),
                calculation.getPlanId(),
                calculation.getExecutedAt(),
                items);
    }

    private RequirementItemView toItemView(RequirementItem item) {
        return new RequirementItemView(
                item.type(),
                item.id(),
                item.number(),
                item.name(),
                item.quantity(),
                item.needType(),
                item.unit(),
                item.sources());
    }
}
