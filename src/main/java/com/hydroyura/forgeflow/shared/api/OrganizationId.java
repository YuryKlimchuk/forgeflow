package com.hydroyura.forgeflow.shared.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Общий типизированный идентификатор организации (tenant).
 */
public record OrganizationId(UUID value) {

    public OrganizationId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static OrganizationId generate() {
        return new OrganizationId(UUID.randomUUID());
    }

    public static OrganizationId of(UUID value) {
        return new OrganizationId(value);
    }
}
