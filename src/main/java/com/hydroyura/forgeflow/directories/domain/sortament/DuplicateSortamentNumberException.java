package com.hydroyura.forgeflow.directories.domain.sortament;

/**
 * Сортамент с таким номером уже существует в организации.
 */
public class DuplicateSortamentNumberException extends RuntimeException {

    public DuplicateSortamentNumberException(String number) {
        super("Sortament number already exists: " + number);
    }
}
