package com.hydroyura.forgeflow.technology.domain.equipment;

import com.hydroyura.forgeflow.technology.api.EquipmentId;

/**
 * Оборудование отсутствует, удалено или принадлежит другой организации.
 */
public class EquipmentNotFoundException extends RuntimeException {

    public EquipmentNotFoundException(EquipmentId id) {
        super("Equipment not found: " + id);
    }
}
