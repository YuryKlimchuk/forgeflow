package com.hydroyura.forgeflow.organizations.application;

import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettingsProvider;
import com.hydroyura.forgeflow.organizations.domain.OrganizationRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Objects;

/**
 * Адаптер {@link OrganizationSettingsProvider} поверх доменного порта
 * {@link OrganizationRepository}.
 */
public class OrganizationSettingsProviderAdapter implements OrganizationSettingsProvider {

    private final OrganizationRepository repository;

    public OrganizationSettingsProviderAdapter(OrganizationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public OrganizationSettings findSettings(OrganizationId organizationId) {
        return repository.findSettings(organizationId);
    }
}
