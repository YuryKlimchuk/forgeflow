package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.planning.api.RequirementCalculationId;

/**
 * Расчёт потребностей не найден или принадлежит другой организации.
 */
public class RequirementCalculationNotFoundException extends PlansException {

    public RequirementCalculationNotFoundException(RequirementCalculationId id) {
        super("Requirement calculation not found: " + id.value());
    }
}
