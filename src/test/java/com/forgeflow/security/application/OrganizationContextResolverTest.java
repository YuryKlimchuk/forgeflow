package com.forgeflow.security.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.forgeflow.organizations.api.Affiliation;
import com.forgeflow.organizations.api.OrganizationService;
import com.forgeflow.organizations.api.UserId;
import com.forgeflow.security.api.AuthenticatedUser;
import com.forgeflow.security.api.Realms;
import com.forgeflow.shared.api.OrganizationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationContextResolverTest {

    private OrganizationService organizationService;
    private OrganizationContextResolver resolver;

    @BeforeEach
    void setUp() {
        organizationService = mock(OrganizationService.class);
        resolver = new OrganizationContextResolver(organizationService);
    }

    @Test
    void ownerGetsOrganization() {
        OrganizationId orgId = OrganizationId.generate();
        UserId userId = UserId.of("user-1");
        when(organizationService.getAffiliation(userId)).thenReturn(new Affiliation.OwnerOf(orgId));

        assertThat(resolver.resolve(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS)))
                .contains(orgId);
    }

    @Test
    void memberGetsOrganization() {
        OrganizationId orgId = OrganizationId.generate();
        UserId userId = UserId.of("user-1");
        when(organizationService.getAffiliation(userId)).thenReturn(new Affiliation.MemberOf(orgId));

        assertThat(resolver.resolve(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS)))
                .contains(orgId);
    }

    @Test
    void userWithoutOrganizationGetsNone() {
        UserId userId = UserId.of("user-1");
        when(organizationService.getAffiliation(userId)).thenReturn(new Affiliation.None());

        assertThat(resolver.resolve(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS)))
                .isEmpty();
    }

    @Test
    void adminRealmGetsNoContextEvenWithOrganization() {
        OrganizationId orgId = OrganizationId.generate();
        UserId userId = UserId.of("admin-1");
        when(organizationService.getAffiliation(userId)).thenReturn(new Affiliation.OwnerOf(orgId));

        assertThat(resolver.resolve(new AuthenticatedUser(userId, Realms.FORGEFLOW_ADMINS)))
                .isEmpty();
    }
}
