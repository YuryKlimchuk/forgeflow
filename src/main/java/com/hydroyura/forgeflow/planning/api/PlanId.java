package com.hydroyura.forgeflow.planning.api;

import java.util.Objects;
import java.util.UUID;

public record PlanId(UUID value) {

    public PlanId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static PlanId generate() {
        return new PlanId(UUID.randomUUID());
    }

    public static PlanId of(UUID value) {
        return new PlanId(value);
    }
}
