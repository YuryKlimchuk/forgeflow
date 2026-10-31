package com.hydroyura.forgeflow.specifications.api;

/**
 * Запрос добавления строки спецификации.
 */
public record AddRowRequest(CatalogItemReference item, int quantity) {
}
