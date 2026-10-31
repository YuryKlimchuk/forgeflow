package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.planning.api.PlanId;

/**
 * План отсутствует или принадлежит другой организации.
 */
public class PlanNotFoundException extends PlansException {

    public PlanNotFoundException(PlanId id) {
        super("Plan not found: " + id);
    }
}
