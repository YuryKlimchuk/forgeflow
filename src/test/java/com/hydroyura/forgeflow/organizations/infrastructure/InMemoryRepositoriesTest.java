package com.hydroyura.forgeflow.organizations.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.organizations.api.OwnerId;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.domain.Organization;
import com.hydroyura.forgeflow.organizations.domain.Owner;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryRepositoriesTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void ownerRepositorySavesAndFinds() {
        InMemoryOwnerRepository repository = new InMemoryOwnerRepository();
        Owner owner = Owner.create(OwnerId.generate(), UserId.of("user-1"));

        repository.save(owner);

        assertThat(repository.findById(owner.getId())).contains(owner);
        assertThat(repository.findByUserId(owner.getUserId())).contains(owner);
        assertThat(repository.findByUserId(UserId.of("unknown"))).isEmpty();
    }

    @Test
    void organizationRepositorySavesAndFindsMember() {
        InMemoryOrganizationRepository repository = new InMemoryOrganizationRepository();
        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);
        UserId member = UserId.of("member-1");
        organization.addMember(member);
        repository.save(organization);

        assertThat(repository.findById(organization.getId())).contains(organization);
        assertThat(repository.findByMemberUserId(member)).contains(organization);
        assertThat(repository.findByMemberUserId(UserId.of("unknown"))).isEmpty();
    }

    @Test
    void organizationRepositoryFindsSettings() {
        InMemoryOrganizationRepository repository = new InMemoryOrganizationRepository();
        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);
        OrganizationSettings settings = new OrganizationSettings("P-\\d+", null, null, null, null);
        organization.replaceSettings(settings, NOW);
        repository.save(organization);

        assertThat(repository.findSettings(organization.getId())).isEqualTo(settings);
        assertThat(repository.findSettings(OrganizationId.generate()))
                .isEqualTo(OrganizationSettings.empty());
    }

    @Test
    void separateInstancesDoNotShareData() {
        InMemoryOrganizationRepository first = new InMemoryOrganizationRepository();
        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);
        first.save(organization);

        InMemoryOrganizationRepository second = new InMemoryOrganizationRepository();

        assertThat(second.findById(organization.getId())).isEmpty();
    }
}
