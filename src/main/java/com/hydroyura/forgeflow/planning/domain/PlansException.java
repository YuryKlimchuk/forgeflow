package com.hydroyura.forgeflow.planning.domain;

/**
 * Базовая ошибка модуля планирования.
 */
public abstract class PlansException extends RuntimeException {

    protected PlansException(String message) {
        super(message);
    }
}
