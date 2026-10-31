package com.forgeflow.catalog.api;

/**
 * Входной контракт создания детали. Поля {@code id}, {@code organizationId} и
 * временные метки являются системными и не передаются клиентом.
 */
public record CreatePartRequest(String number, String name) {
}
