package com.hydroyura.forgeflow.catalog.application;

/**
 * В текущей организации уже существует активная сборочная единица с таким номером.
 */
public class DuplicateAssemblyNumberException extends CatalogException {

    public DuplicateAssemblyNumberException(String number) {
        super("Assembly number already exists: " + number);
    }
}
