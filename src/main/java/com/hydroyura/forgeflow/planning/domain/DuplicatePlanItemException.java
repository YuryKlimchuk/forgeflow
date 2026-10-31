package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;

/**
 * Сборка уже включена в план.
 */
public class DuplicatePlanItemException extends PlansException {

    public DuplicatePlanItemException(AssemblyId assemblyId) {
        super("Assembly already in plan: " + assemblyId);
    }
}
