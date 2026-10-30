package com.forgeflow.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgeflow.shared.api.OrganizationContext;
import com.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.forgeflow.shared.api.OrganizationId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CatalogOrganizationScopeServiceTest {

    @Test
    void passesOrganizationIdToRepository() {
        OrganizationId orgId = OrganizationId.generate();
        OrganizationContext context = fakeContext(orgId);
        AtomicReference<OrganizationId> received = new AtomicReference<>();
        OrganizationScopedRepository repository = id -> {
            received.set(id);
            return 42;
        };
        CatalogOrganizationScopeService service = new CatalogOrganizationScopeService(context, repository);

        int result = service.countAssemblies();

        assertThat(result).isEqualTo(42);
        assertThat(received.get()).isEqualTo(orgId);
    }

    @Test
    void throwsWhenNoContext() {
        CatalogOrganizationScopeService service = new CatalogOrganizationScopeService(
                fakeContext(null),
                id -> 0);

        assertThatThrownBy(service::countAssemblies)
                .isInstanceOf(OrganizationContextNotAvailableException.class);
    }

    private static OrganizationContext fakeContext(OrganizationId id) {
        return new OrganizationContext() {
            @Override
            public Optional<OrganizationId> currentOrganizationId() {
                return Optional.ofNullable(id);
            }

            @Override
            public OrganizationId requiredOrganizationId() {
                if (id == null) {
                    throw new OrganizationContextNotAvailableException("no context");
                }
                return id;
            }
        };
    }
}
