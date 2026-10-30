package com.forgeflow.organizations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgeflow.shared.api.OrganizationId;
import com.forgeflow.organizations.api.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrganizationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void createWithNameAndAudit() {
        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);

        assertThat(organization.getName()).isEqualTo("Acme");
        assertThat(organization.getCreatedAt()).isEqualTo(NOW);
        assertThat(organization.getUpdatedAt()).isEqualTo(NOW);
        assertThat(organization.getMembers()).isEmpty();
    }

    @Test
    void rejectBlankNameOnCreateAndRename() {
        assertThatThrownBy(() -> Organization.create(OrganizationId.generate(), "   ", NOW))
                .isInstanceOf(IllegalArgumentException.class);

        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);
        assertThatThrownBy(() -> organization.rename("", LATER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renameUpdatesNameAndUpdatedAtOnly() {
        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);

        organization.rename("Acme Inc", LATER);

        assertThat(organization.getName()).isEqualTo("Acme Inc");
        assertThat(organization.getUpdatedAt()).isEqualTo(LATER);
        assertThat(organization.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void addMemberAndRejectDuplicate() {
        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);
        UserId userId = UserId.of("user-1");

        organization.addMember(userId);

        assertThat(organization.hasMember(userId)).isTrue();
        assertThat(organization.getMembers()).hasSize(1);
        assertThat(organization.getMembers().get(0).userId()).isEqualTo(userId);

        assertThatThrownBy(() -> organization.addMember(userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeMemberAndRejectUnknown() {
        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);
        UserId userId = UserId.of("user-1");
        organization.addMember(userId);

        organization.removeMember(userId);

        assertThat(organization.hasMember(userId)).isFalse();
        assertThat(organization.getMembers()).isEmpty();

        assertThatThrownBy(() -> organization.removeMember(userId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
