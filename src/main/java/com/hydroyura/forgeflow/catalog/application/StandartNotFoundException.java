package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.StandartId;

/**
 * Стандартное изделие отсутствует, удалено или принадлежит другой организации.
 */
public class StandartNotFoundException extends CatalogException {

    public StandartNotFoundException(StandartId id) {
        super("Standart not found: " + id);
    }
}
