package com.hydroyura.forgeflow.catalog.application;

/**
 * В текущей организации уже существует активная группа ВЗК с таким номером.
 */
public class DuplicateVzkNumberException extends CatalogException {

    public DuplicateVzkNumberException(String number) {
        super("Vzk number already exists: " + number);
    }
}
