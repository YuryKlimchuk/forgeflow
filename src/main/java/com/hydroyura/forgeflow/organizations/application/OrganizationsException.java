package com.hydroyura.forgeflow.organizations.application;

/**
 * Базовая ошибка бизнес-операций модуля организаций.
 */
public abstract class OrganizationsException extends RuntimeException {

    protected OrganizationsException(String message) {
        super(message);
    }
}
