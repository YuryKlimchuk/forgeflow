package com.hydroyura.forgeflow.technology.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Типизированный идентификатор технологического процесса.
 */
public record TechnologyProcessId(UUID value) {

    public TechnologyProcessId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static TechnologyProcessId generate() {
        return new TechnologyProcessId(UUID.randomUUID());
    }

    public static TechnologyProcessId of(UUID value) {
        return new TechnologyProcessId(value);
    }
}
