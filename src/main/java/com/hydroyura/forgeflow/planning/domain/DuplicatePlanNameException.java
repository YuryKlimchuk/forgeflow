package com.hydroyura.forgeflow.planning.domain;

/**
 * План с таким именем уже существует в организации.
 */
public class DuplicatePlanNameException extends PlansException {

    public DuplicatePlanNameException(String name) {
        super("Plan name already exists: " + name);
    }
}
