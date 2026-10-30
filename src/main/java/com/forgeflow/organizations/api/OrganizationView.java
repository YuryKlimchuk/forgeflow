package com.forgeflow.organizations.api;

import com.forgeflow.shared.api.OrganizationId;
import java.time.Instant;

/**
 * Проекция организации для чтения.
 */
public record OrganizationView(
        OrganizationId id,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
