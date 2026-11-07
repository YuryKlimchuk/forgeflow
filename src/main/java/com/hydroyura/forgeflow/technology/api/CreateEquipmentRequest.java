package com.hydroyura.forgeflow.technology.api;

/**
 * Входной контракт создания оборудования. Поля {@code id}, {@code organizationId}
 * и временные метки являются системными и не передаются клиентом.
 */
public record CreateEquipmentRequest(String number, String name, String type) {
}
