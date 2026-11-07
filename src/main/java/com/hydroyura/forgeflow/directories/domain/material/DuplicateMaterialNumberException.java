package com.hydroyura.forgeflow.directories.domain.material;

/**
 * Материал с таким номером уже существует в организации.
 */
public class DuplicateMaterialNumberException extends RuntimeException {

    public DuplicateMaterialNumberException(String number) {
        super("Material number already exists: " + number);
    }
}
