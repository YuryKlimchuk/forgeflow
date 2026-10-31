package com.hydroyura.forgeflow.catalog.api;

import java.time.Instant;

/**
 * Проекция сборочной единицы для чтения (исходящий DTO).
 */
public record AssemblyView(
        AssemblyId id,
        String number,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
