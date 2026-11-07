package com.hydroyura.forgeflow.technology.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Типизированный идентификатор оборудования.
 */
public record EquipmentId(UUID value) {

    public EquipmentId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static EquipmentId generate() {
        return new EquipmentId(UUID.randomUUID());
    }

    public static EquipmentId of(UUID value) {
        return new EquipmentId(value);
    }
}
