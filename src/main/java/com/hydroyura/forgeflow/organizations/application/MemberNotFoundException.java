package com.hydroyura.forgeflow.organizations.application;

/**
 * Пользователь не является участником организации.
 */
public class MemberNotFoundException extends OrganizationsException {

    public MemberNotFoundException(String message) {
        super(message);
    }
}
