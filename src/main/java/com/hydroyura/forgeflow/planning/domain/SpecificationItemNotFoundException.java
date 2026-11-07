package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;

/**
 * Строка спецификации ссылается на отсутствующую или чужую вложенную сборку.
 */
public class SpecificationItemNotFoundException extends PlansException {

    private final CatalogItemReference item;

    public SpecificationItemNotFoundException(CatalogItemReference item) {
        super("Specification item not found: " + item.type() + " " + item.id());
        this.item = item;
    }

    public CatalogItemReference item() {
        return item;
    }
}
