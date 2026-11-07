package com.hydroyura.forgeflow.directories.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Типизированный идентификатор сортамента.
 */
public record SortamentId(UUID value) {

    public SortamentId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static SortamentId generate() {
        return new SortamentId(UUID.randomUUID());
    }

    public static SortamentId of(UUID value) {
        return new SortamentId(value);
    }
}
