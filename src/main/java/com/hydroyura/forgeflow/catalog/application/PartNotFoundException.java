package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.PartId;

/**
 * Деталь отсутствует, удалена или принадлежит другой организации.
 */
public class PartNotFoundException extends CatalogException {

    public PartNotFoundException(PartId id) {
        super("Part not found: " + id);
    }
}
