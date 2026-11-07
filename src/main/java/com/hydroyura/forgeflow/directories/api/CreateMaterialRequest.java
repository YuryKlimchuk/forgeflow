package com.hydroyura.forgeflow.directories.api;

/**
 * Входной контракт создания материала. Поля {@code id}, {@code organizationId}
 * и временные метки являются системными и не передаются клиентом.
 */
public record CreateMaterialRequest(
        String number,
        String name,
        StandardType standardType,
        String standard,
        String unit) {
}
