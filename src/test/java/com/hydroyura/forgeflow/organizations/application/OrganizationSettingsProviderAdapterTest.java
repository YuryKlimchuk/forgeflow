package com.hydroyura.forgeflow.organizations.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettingsProvider;
import com.hydroyura.forgeflow.organizations.domain.Organization;
import com.hydroyura.forgeflow.organizations.infrastructure.InMemoryOrganizationRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrganizationSettingsProviderAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final InMemoryOrganizationRepository repository = new InMemoryOrganizationRepository();
    private final OrganizationSettingsProvider provider = new OrganizationSettingsProviderAdapter(repository);

    @Test
    void returnsSettingsForOrganization() {
        Organization organization = Organization.create(OrganizationId.generate(), "Acme", NOW);
        OrganizationSettings settings = new OrganizationSettings("P-\\d+", null, null, null, null);
        organization.replaceSettings(settings, NOW);
        repository.save(organization);

        assertThat(provider.findSettings(organization.getId())).isEqualTo(settings);
    }

    @Test
    void returnsEmptyForUnknownOrganization() {
        assertThat(provider.findSettings(OrganizationId.generate()))
                .isEqualTo(OrganizationSettings.empty());
    }

    @Test
    void settingsAreIsolatedBetweenOrganizations() {
        Organization orgA = Organization.create(OrganizationId.generate(), "A", NOW);
        orgA.replaceSettings(new OrganizationSettings("A-\\d+", null, null, null, null), NOW);
        repository.save(orgA);

        Organization orgB = Organization.create(OrganizationId.generate(), "B", NOW);
        repository.save(orgB);

        assertThat(provider.findSettings(orgA.getId()).partNumberPattern()).isEqualTo("A-\\d+");
        assertThat(provider.findSettings(orgB.getId())).isEqualTo(OrganizationSettings.empty());
    }
}
