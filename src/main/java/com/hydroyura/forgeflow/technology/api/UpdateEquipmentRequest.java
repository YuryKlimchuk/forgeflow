package com.hydroyura.forgeflow.technology.api;

/**
 * Входной контракт обновления изменяемых полей оборудования. Поле {@code number}
 * неизменяемо и не входит в тело запроса.
 */
public record UpdateEquipmentRequest(String name, String type) {
}
