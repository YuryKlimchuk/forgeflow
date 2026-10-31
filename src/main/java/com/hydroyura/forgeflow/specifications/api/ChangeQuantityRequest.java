package com.hydroyura.forgeflow.specifications.api;

/**
 * Запрос изменения количества строки спецификации.
 */
public record ChangeQuantityRequest(CatalogItemReference item, int quantity) {
}
