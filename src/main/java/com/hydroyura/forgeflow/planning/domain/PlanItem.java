package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import java.util.Objects;

/**
 * Элемент плана: сборка и положительное целое количество штук.
 */
public record PlanItem(AssemblyId assemblyId, int quantity) {

    public PlanItem {
        Objects.requireNonNull(assemblyId, "assemblyId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
