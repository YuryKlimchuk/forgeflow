package com.forgeflow.security.application;

import com.forgeflow.organizations.api.Affiliation;
import com.forgeflow.organizations.api.OrganizationService;
import com.forgeflow.security.api.AuthenticatedUser;
import com.forgeflow.security.api.Realms;
import com.forgeflow.shared.api.OrganizationId;
import java.util.Objects;
import java.util.Optional;

/**
 * Определяет организацию по аутентифицированному пользователю.
 *
 * <p>Для realm {@code forgeflow-users} организация берётся из модуля
 * {@code organizations} по явному subject; для {@code forgeflow-admins}
 * и неизвестных realm'ов контекст организации не предоставляется.</p>
 */
public class OrganizationContextResolver {

    private final OrganizationService organizationService;

    public OrganizationContextResolver(OrganizationService organizationService) {
        this.organizationService = Objects.requireNonNull(organizationService, "organizationService must not be null");
    }

    public Optional<OrganizationId> resolve(AuthenticatedUser user) {
        if (!Realms.FORGEFLOW_USERS.equals(user.realm())) {
            return Optional.empty();
        }
        Affiliation affiliation = organizationService.getAffiliation(user.userId());
        return switch (affiliation) {
            case Affiliation.OwnerOf(var organizationId) -> Optional.of(organizationId);
            case Affiliation.MemberOf(var organizationId) -> Optional.of(organizationId);
            case Affiliation.None() -> Optional.empty();
        };
    }
}
