package com.hydroyura.forgeflow.technology.domain;

/**
 * Операция с указанным номером отсутствует в технологическом процессе.
 */
public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(int number) {
        super("Operation not found with number: " + number);
    }
}
