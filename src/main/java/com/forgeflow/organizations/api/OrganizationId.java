package com.forgeflow.organizations.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Типизированный идентификатор организации.
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
