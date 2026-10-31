package com.forgeflow.catalog.application;

/**
 * В текущей организации уже существует активная деталь с таким номером.
 */
public class DuplicatePartNumberException extends CatalogException {

    public DuplicatePartNumberException(String number) {
        super("Part number already exists: " + number);
    }
}
