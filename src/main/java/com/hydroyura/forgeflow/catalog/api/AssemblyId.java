package com.hydroyura.forgeflow.catalog.api;

import java.util.Objects;
import java.util.UUID;

public record AssemblyId(UUID value) {

    public AssemblyId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static AssemblyId generate() {
        return new AssemblyId(UUID.randomUUID());
    }

    public static AssemblyId of(UUID value) {
        return new AssemblyId(value);
    }
}
