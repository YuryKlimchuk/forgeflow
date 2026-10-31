package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;

/**
 * Элемент каталога уже присутствует в спецификации.
 */
public class DuplicateItemException extends SpecificationsException {

    public DuplicateItemException(CatalogItemReference item) {
        super("Item already present in specification: " + item.type() + " " + item.id());
    }
}
