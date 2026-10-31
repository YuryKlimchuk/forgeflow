package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.VzkId;

/**
 * Группа ВЗК отсутствует, удалена или принадлежит другой организации.
 */
public class VzkNotFoundException extends CatalogException {

    public VzkNotFoundException(VzkId id) {
        super("Vzk not found: " + id);
    }
}
