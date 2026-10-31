package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;

/**
 * Строка спецификации не найдена.
 */
public class RowNotFoundException extends SpecificationsException {

    public RowNotFoundException(CatalogItemReference item) {
        super("Specification row not found: " + item.type() + " " + item.id());
    }
}
