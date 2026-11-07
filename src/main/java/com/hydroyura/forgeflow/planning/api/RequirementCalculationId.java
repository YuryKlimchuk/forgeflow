package com.hydroyura.forgeflow.planning.api;

import java.util.Objects;
import java.util.UUID;

public record RequirementCalculationId(UUID value) {

    public RequirementCalculationId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static RequirementCalculationId generate() {
        return new RequirementCalculationId(UUID.randomUUID());
    }

    public static RequirementCalculationId of(UUID value) {
        return new RequirementCalculationId(value);
    }
}
