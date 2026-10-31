package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import java.util.Objects;

/**
 * Строка спецификации: ссылка на элемент каталога и количество.
 */
public record SpecificationRow(CatalogItemReference item, Quantity quantity) {

    public SpecificationRow {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
    }
}
