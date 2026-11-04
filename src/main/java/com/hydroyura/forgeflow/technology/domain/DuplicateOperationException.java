package com.hydroyura.forgeflow.technology.domain;

/**
 * Операция с таким номером уже добавлена в технологический процесс.
 */
public class DuplicateOperationException extends RuntimeException {

    public DuplicateOperationException(int number) {
        super("Operation already exists with number: " + number);
    }
}
