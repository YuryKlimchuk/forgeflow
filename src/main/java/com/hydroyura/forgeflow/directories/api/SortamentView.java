package com.hydroyura.forgeflow.directories.api;

import java.time.Instant;

/**
 * Представление сортамента для чтения.
 */
public record SortamentView(
        SortamentId id,
        String number,
        String name,
        StandardType standardType,
        String standard,
        String unit,
        Instant createdAt,
        Instant updatedAt) {
}
