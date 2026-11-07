package com.hydroyura.forgeflow.directories.api;

/**
 * Входной контракт обновления изменяемых полей сортамента. Поле {@code number}
 * неизменяемо и не входит в тело запроса.
 */
public record UpdateSortamentRequest(
        String name,
        StandardType standardType,
        String standard,
        String unit) {
}
