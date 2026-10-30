package com.forgeflow.security.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.forgeflow.organizations.api.Affiliation;
import com.forgeflow.organizations.api.OrganizationService;
import com.forgeflow.organizations.api.UserId;
import com.forgeflow.security.api.AuthenticatedUser;
import com.forgeflow.security.api.Realms;
import com.forgeflow.security.application.OrganizationContextResolver;
import com.forgeflow.shared.api.OrganizationId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationContextLifecycleTest {

    private OrganizationService organizationService;
    private ThreadLocalOrganizationContext context;
    private StubAuthenticatedUserProvider userProvider;
    private OrganizationContextLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        organizationService = mock(OrganizationService.class);
        context = new ThreadLocalOrganizationContext();
        userProvider = new StubAuthenticatedUserProvider();
        lifecycle = new OrganizationContextLifecycle(
                userProvider,
                new OrganizationContextResolver(organizationService),
                context);
    }

    @Test
    void handlerSeesResolvedOrganizationAndContextIsCleared() {
        OrganizationId orgId = OrganizationId.generate();
        UserId userId = UserId.of("user-1");
        when(organizationService.getAffiliation(userId)).thenReturn(new Affiliation.OwnerOf(orgId));
        userProvider.set(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS));

        AtomicReference<OrganizationId> seen = new AtomicReference<>();
        lifecycle.run(() -> {
            seen.set(context.requiredOrganizationId());
            return null;
        });

        assertThat(seen.get()).isEqualTo(orgId);
        assertThat(context.currentOrganizationId()).isEmpty();
    }

    @Test
    void exceptionInHandlerClearsContext() {
        OrganizationId orgId = OrganizationId.generate();
        UserId userId = UserId.of("user-1");
        when(organizationService.getAffiliation(userId)).thenReturn(new Affiliation.MemberOf(orgId));
        userProvider.set(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS));

        assertThatThrownBy(() -> lifecycle.run((Runnable) () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(context.currentOrganizationId()).isEmpty();
    }

    @Test
    void sequentialRequestsDoNotLeakContext() {
        OrganizationId orgId = OrganizationId.generate();
        UserId userId = UserId.of("user-1");
        when(organizationService.getAffiliation(userId)).thenReturn(new Affiliation.OwnerOf(orgId));
        userProvider.set(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS));

        lifecycle.run(() -> {
            assertThat(context.requiredOrganizationId()).isEqualTo(orgId);
            return null;
        });

        userProvider.clear();
        lifecycle.run(() -> {
            assertThat(context.currentOrganizationId()).isEmpty();
            return null;
        });
    }

    @Test
    void adminRealmDoesNotSetContext() {
        userProvider.set(new AuthenticatedUser(UserId.of("admin-1"), Realms.FORGEFLOW_ADMINS));

        lifecycle.run(() -> {
            assertThat(context.currentOrganizationId()).isEmpty();
            return null;
        });
    }
}
