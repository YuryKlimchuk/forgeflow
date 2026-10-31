package com.hydroyura.forgeflow.specifications.domain;

/**
 * Положительное целое количество штук.
 */
public record Quantity(int value) {

    public Quantity {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
