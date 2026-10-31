package com.hydroyura.forgeflow.catalog.api;

import java.time.Instant;

/**
 * Проекция стандартного изделия для чтения (исходящий DTO).
 */
public record StandartView(
        StandartId id,
        String number,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
