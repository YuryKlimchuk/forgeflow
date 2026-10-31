package com.hydroyura.forgeflow.catalog.api;

import java.util.Objects;
import java.util.UUID;

public record VzkId(UUID value) {

    public VzkId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static VzkId generate() {
        return new VzkId(UUID.randomUUID());
    }

    public static VzkId of(UUID value) {
        return new VzkId(value);
    }
}
