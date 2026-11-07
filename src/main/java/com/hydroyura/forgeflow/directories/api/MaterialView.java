package com.hydroyura.forgeflow.directories.api;

import java.time.Instant;

/**
 * Представление материала для чтения.
 */
public record MaterialView(
        MaterialId id,
        String number,
        String name,
        StandardType standardType,
        String standard,
        String unit,
        Instant createdAt,
        Instant updatedAt) {
}
