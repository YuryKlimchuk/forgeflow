package com.hydroyura.forgeflow.technology.api;

import java.time.Instant;

/**
 * Представление оборудования для чтения.
 */
public record EquipmentView(
        EquipmentId id,
        String number,
        String name,
        String type,
        Instant createdAt,
        Instant updatedAt) {
}
