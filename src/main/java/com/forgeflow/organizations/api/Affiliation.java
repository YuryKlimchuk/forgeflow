package com.forgeflow.organizations.api;

/**
 * Принадлежность текущего пользователя: владелец организации, участник
 * организации либо отсутствие организации.
 */
public sealed interface Affiliation {

    record OwnerOf(OrganizationId organizationId) implements Affiliation {
    }

    record MemberOf(OrganizationId organizationId) implements Affiliation {
    }

    record None() implements Affiliation {
    }
}
