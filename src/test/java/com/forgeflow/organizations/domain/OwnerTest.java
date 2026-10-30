package com.forgeflow.organizations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgeflow.shared.api.OrganizationId;
import com.forgeflow.organizations.api.OwnerId;
import com.forgeflow.organizations.api.UserId;
import org.junit.jupiter.api.Test;

class OwnerTest {

    @Test
    void ownerIsCreatedWithoutOrganization() {
        Owner owner = Owner.create(OwnerId.generate(), UserId.of("user-1"));

        assertThat(owner.getUserId()).isEqualTo(UserId.of("user-1"));
        assertThat(owner.getOrganizationId()).isNull();
    }

    @Test
    void ownerBindsToOrganizationOnlyOnce() {
        Owner owner = Owner.create(OwnerId.generate(), UserId.of("user-1"));
        OrganizationId organizationId = OrganizationId.generate();

        owner.bind(organizationId);
        assertThat(owner.getOrganizationId()).isEqualTo(organizationId);

        assertThatThrownBy(() -> owner.bind(OrganizationId.generate()))
                .isInstanceOf(IllegalStateException.class);
    }
}
