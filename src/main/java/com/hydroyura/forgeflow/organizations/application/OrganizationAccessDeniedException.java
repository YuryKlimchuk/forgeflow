package com.hydroyura.forgeflow.organizations.application;

/**
 * Пользователь не имеет доступа к организации (не владелец и не участник).
 */
public class OrganizationAccessDeniedException extends OrganizationsException {

    public OrganizationAccessDeniedException(String message) {
        super(message);
    }
}
