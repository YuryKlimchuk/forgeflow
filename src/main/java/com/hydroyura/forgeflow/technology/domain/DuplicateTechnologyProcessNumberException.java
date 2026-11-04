package com.hydroyura.forgeflow.technology.domain;

/**
 * Технологический процесс с таким номером уже существует в организации.
 */
public class DuplicateTechnologyProcessNumberException extends RuntimeException {

    public DuplicateTechnologyProcessNumberException(String number) {
        super("Technology process number already exists: " + number);
    }
}
