package com.hydroyura.forgeflow.specifications.domain;

/**
 * Базовая ошибка модуля спецификаций.
 */
public abstract class SpecificationsException extends RuntimeException {

    protected SpecificationsException(String message) {
        super(message);
    }
}
