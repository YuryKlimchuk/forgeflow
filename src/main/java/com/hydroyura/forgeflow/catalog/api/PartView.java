package com.hydroyura.forgeflow.catalog.api;

import java.time.Instant;

/**
 * Проекция детали для чтения (исходящий DTO).
 */
public record PartView(
        PartId id,
        String number,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
