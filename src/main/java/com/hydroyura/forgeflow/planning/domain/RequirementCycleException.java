package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;

/**
 * В спецификациях обнаружен цикл вложенных сборок.
 */
public class RequirementCycleException extends PlansException {

    public RequirementCycleException(AssemblyId assemblyId) {
        super("Requirement cycle detected at assembly: " + assemblyId.value());
    }
}
