package com.hydroyura.forgeflow.technology.domain.equipment;

/**
 * Оборудование с таким номером уже существует в организации.
 */
public class DuplicateEquipmentNumberException extends RuntimeException {

    public DuplicateEquipmentNumberException(String number) {
        super("Equipment number already exists: " + number);
    }
}
