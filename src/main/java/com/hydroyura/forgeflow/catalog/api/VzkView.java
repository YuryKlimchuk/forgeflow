package com.hydroyura.forgeflow.catalog.api;

import java.time.Instant;

/**
 * Проекция группы ВЗК для чтения (исходящий DTO).
 */
public record VzkView(
        VzkId id,
        String number,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
