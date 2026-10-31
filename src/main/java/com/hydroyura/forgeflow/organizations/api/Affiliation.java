package com.hydroyura.forgeflow.organizations.api;

import com.hydroyura.forgeflow.shared.api.OrganizationId;

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
