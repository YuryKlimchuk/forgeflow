package com.hydroyura.forgeflow.specifications.api;

/**
 * Запрос добавления доп. замены к строке спецификации.
 */
public record AddSubstituteRequest(CatalogItemReference item, CatalogItemReference substituteRef) {
}
