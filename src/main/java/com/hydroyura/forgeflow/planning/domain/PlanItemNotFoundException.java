package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;

/**
 * Сборка отсутствует в составе плана.
 */
public class PlanItemNotFoundException extends PlansException {

    public PlanItemNotFoundException(AssemblyId assemblyId) {
        super("Plan item not found: " + assemblyId);
    }
}
