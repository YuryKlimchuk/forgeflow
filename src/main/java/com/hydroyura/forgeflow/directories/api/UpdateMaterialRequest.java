package com.hydroyura.forgeflow.directories.api;

/**
 * Входной контракт обновления изменяемых полей материала. Поле {@code number}
 * неизменяемо и не входит в тело запроса.
 */
public record UpdateMaterialRequest(
        String name,
        StandardType standardType,
        String standard,
        String unit) {
}
