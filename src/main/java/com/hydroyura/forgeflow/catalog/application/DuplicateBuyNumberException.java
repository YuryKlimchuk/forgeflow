package com.hydroyura.forgeflow.catalog.application;

/**
 * В текущей организации уже существует активное покупное изделие с таким номером.
 */
public class DuplicateBuyNumberException extends CatalogException {

    public DuplicateBuyNumberException(String number) {
        super("Buy number already exists: " + number);
    }
}
