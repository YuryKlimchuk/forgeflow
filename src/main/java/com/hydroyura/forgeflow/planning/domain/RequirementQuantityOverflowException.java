package com.hydroyura.forgeflow.planning.domain;

/**
 * Переполнение количества при расчёте потребностей.
 */
public class RequirementQuantityOverflowException extends PlansException {

    public RequirementQuantityOverflowException(String message) {
        super(message);
    }
}
