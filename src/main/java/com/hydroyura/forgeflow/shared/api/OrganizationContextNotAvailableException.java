package com.hydroyura.forgeflow.shared.api;

/**
 * Контекст текущей организации отсутствует.
 */
public class OrganizationContextNotAvailableException extends RuntimeException {

    public OrganizationContextNotAvailableException(String message) {
        super(message);
    }
}
