package com.hydroyura.forgeflow.directories.api;

/**
 * Входной контракт создания сортамента. Поля {@code id}, {@code organizationId}
 * и временные метки являются системными и не передаются клиентом.
 */
public record CreateSortamentRequest(
        String number,
        String name,
        StandardType standardType,
        String standard,
        String unit) {
}
