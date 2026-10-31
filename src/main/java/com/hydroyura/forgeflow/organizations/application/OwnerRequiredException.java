package com.hydroyura.forgeflow.organizations.application;

/**
 * Операция требует роли владельца.
 */
public class OwnerRequiredException extends OrganizationsException {

    public OwnerRequiredException(String message) {
        super(message);
    }
}
