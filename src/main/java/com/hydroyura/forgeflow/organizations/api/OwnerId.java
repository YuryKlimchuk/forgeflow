package com.hydroyura.forgeflow.organizations.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Типизированный идентификатор владельца организации.
 */
public record OwnerId(UUID value) {

    public OwnerId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static OwnerId generate() {
        return new OwnerId(UUID.randomUUID());
    }

    public static OwnerId of(UUID value) {
        return new OwnerId(value);
    }
}
