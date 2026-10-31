package com.hydroyura.forgeflow.organizations.application;

/**
 * Организация не найдена.
 */
public class OrganizationNotFoundException extends OrganizationsException {

    public OrganizationNotFoundException(String message) {
        super(message);
    }
}
