package com.hydroyura.forgeflow.catalog.application;

/**
 * В текущей организации уже существует активное стандартное изделие с таким номером.
 */
public class DuplicateStandartNumberException extends CatalogException {

    public DuplicateStandartNumberException(String number) {
        super("Standart number already exists: " + number);
    }
}
