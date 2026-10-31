package com.hydroyura.forgeflow.catalog.api;

import java.util.Objects;
import java.util.UUID;

public record PartId(UUID value) {

    public PartId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static PartId generate() {
        return new PartId(UUID.randomUUID());
    }

    public static PartId of(UUID value) {
        return new PartId(value);
    }
}
