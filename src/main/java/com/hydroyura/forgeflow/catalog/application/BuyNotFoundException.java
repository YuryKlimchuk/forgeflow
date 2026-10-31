package com.hydroyura.forgeflow.catalog.application;

import com.hydroyura.forgeflow.catalog.api.BuyId;

/**
 * Покупное изделие отсутствует, удалено или принадлежит другой организации.
 */
public class BuyNotFoundException extends CatalogException {

    public BuyNotFoundException(BuyId id) {
        super("Buy not found: " + id);
    }
}
