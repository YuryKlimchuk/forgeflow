package com.hydroyura.forgeflow.organizations.application;

/**
 * Пользователь уже является владельцем или участником организации.
 */
public class UserAlreadyOccupiedException extends OrganizationsException {

    public UserAlreadyOccupiedException(String message) {
        super(message);
    }
}
