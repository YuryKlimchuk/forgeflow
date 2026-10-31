package com.hydroyura.forgeflow.specifications.api;

/**
 * Запрос удаления доп. замены у строки спецификации.
 */
public record RemoveSubstituteRequest(CatalogItemReference item, CatalogItemReference substituteRef) {
}
