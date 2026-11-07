package com.hydroyura.forgeflow.directories.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Типизированный идентификатор материала.
 */
public record MaterialId(UUID value) {

    public MaterialId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static MaterialId generate() {
        return new MaterialId(UUID.randomUUID());
    }

    public static MaterialId of(UUID value) {
        return new MaterialId(value);
    }
}
