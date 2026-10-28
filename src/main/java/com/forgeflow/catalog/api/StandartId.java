package com.forgeflow.catalog.api;

import java.util.Objects;
import java.util.UUID;

public record StandartId(UUID value) {

    public StandartId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static StandartId generate() {
        return new StandartId(UUID.randomUUID());
    }

    public static StandartId of(UUID value) {
        return new StandartId(value);
    }
}
