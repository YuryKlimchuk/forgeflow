package com.forgeflow.security.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgeflow.shared.api.OrganizationContextNotAvailableException;
import com.forgeflow.shared.api.OrganizationId;
import org.junit.jupiter.api.Test;

class ThreadLocalOrganizationContextTest {

    @Test
    void setGetAndClear() {
        ThreadLocalOrganizationContext context = new ThreadLocalOrganizationContext();
        OrganizationId id = OrganizationId.generate();

        context.set(id);
        assertThat(context.currentOrganizationId()).contains(id);
        assertThat(context.requiredOrganizationId()).isEqualTo(id);

        context.clear();
        assertThat(context.currentOrganizationId()).isEmpty();
    }

    @Test
    void emptyOptionalWhenNotSet() {
        assertThat(new ThreadLocalOrganizationContext().currentOrganizationId()).isEmpty();
    }

    @Test
    void requiredThrowsWhenNotSet() {
        ThreadLocalOrganizationContext context = new ThreadLocalOrganizationContext();

        assertThatThrownBy(context::requiredOrganizationId)
                .isInstanceOf(OrganizationContextNotAvailableException.class);
    }
}
