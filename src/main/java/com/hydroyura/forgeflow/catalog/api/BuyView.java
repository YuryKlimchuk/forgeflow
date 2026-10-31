package com.hydroyura.forgeflow.catalog.api;

import java.time.Instant;

/**
 * Проекция покупного изделия для чтения (исходящий DTO).
 */
public record BuyView(
        BuyId id,
        String number,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
