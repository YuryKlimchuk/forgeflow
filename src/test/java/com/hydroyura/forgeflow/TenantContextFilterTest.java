package com.hydroyura.forgeflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hydroyura.forgeflow.organizations.api.Affiliation;
import com.hydroyura.forgeflow.organizations.api.OrganizationService;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.security.api.AuthenticatedUser;
import com.hydroyura.forgeflow.security.api.Realms;
import com.hydroyura.forgeflow.security.application.OrganizationContextResolver;
import com.hydroyura.forgeflow.security.infrastructure.OrganizationContextLifecycle;
import com.hydroyura.forgeflow.security.infrastructure.StubAuthenticatedUserProvider;
import com.hydroyura.forgeflow.security.infrastructure.ThreadLocalOrganizationContext;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantContextFilterTest {

    @Test
    void resolvesOrganizationFromAuthenticatedUserAndClearsAfterwards() throws Exception {
        OrganizationId orgId = OrganizationId.generate();
        UserId userId = UserId.of("user-1");

        OrganizationService organizationService = mock(OrganizationService.class);
        when(organizationService.getAffiliation(userId)).thenReturn(new Affiliation.OwnerOf(orgId));

        StubAuthenticatedUserProvider userProvider = new StubAuthenticatedUserProvider();
        userProvider.set(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS));

        ThreadLocalOrganizationContext context = new ThreadLocalOrganizationContext();
        TenantContextFilter filter = filter(userProvider, organizationService, context);

        AtomicReference<Optional<OrganizationId>> duringRequest = new AtomicReference<>();
        FilterChain chain = (req, res) -> duringRequest.set(context.currentOrganizationId());

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(duringRequest.get()).contains(orgId);
        assertThat(context.currentOrganizationId()).isEmpty();
    }

    @Test
    void ignoresClientOrganizationHeader() throws Exception {
        ThreadLocalOrganizationContext context = new ThreadLocalOrganizationContext();
        StubAuthenticatedUserProvider userProvider = new StubAuthenticatedUserProvider();
        // пользователь НЕ задан — заголовок не должен давать организацию
        TenantContextFilter filter = filter(userProvider, mock(OrganizationService.class), context);

        AtomicReference<Optional<OrganizationId>> duringRequest = new AtomicReference<>();
        FilterChain chain = (req, res) -> duringRequest.set(context.currentOrganizationId());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Organization-Id", OrganizationId.generate().toString());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(duringRequest.get()).isEmpty();
    }

    @Test
    void clearsContextEvenWhenChainThrows() throws Exception {
        OrganizationId orgId = OrganizationId.generate();
        UserId userId = UserId.of("user-1");

        OrganizationService organizationService = mock(OrganizationService.class);
        when(organizationService.getAffiliation(userId)).thenReturn(new Affiliation.OwnerOf(orgId));

        StubAuthenticatedUserProvider userProvider = new StubAuthenticatedUserProvider();
        userProvider.set(new AuthenticatedUser(userId, Realms.FORGEFLOW_USERS));

        ThreadLocalOrganizationContext context = new ThreadLocalOrganizationContext();
        TenantContextFilter filter = filter(userProvider, organizationService, context);

        FilterChain chain = (req, res) -> {
            throw new IOException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
                .isInstanceOf(IOException.class);
        assertThat(context.currentOrganizationId()).isEmpty();
    }

    private static TenantContextFilter filter(
            StubAuthenticatedUserProvider userProvider,
            OrganizationService organizationService,
            ThreadLocalOrganizationContext context) {
        OrganizationContextResolver resolver = new OrganizationContextResolver(organizationService);
        OrganizationContextLifecycle lifecycle = new OrganizationContextLifecycle(userProvider, resolver, context);
        return new TenantContextFilter(lifecycle);
    }
}
