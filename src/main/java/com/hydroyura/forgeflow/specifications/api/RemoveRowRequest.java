package com.hydroyura.forgeflow.specifications.api;

/**
 * Запрос удаления строки спецификации (адресуется ссылкой на элемент).
 */
public record RemoveRowRequest(CatalogItemReference item) {
}
