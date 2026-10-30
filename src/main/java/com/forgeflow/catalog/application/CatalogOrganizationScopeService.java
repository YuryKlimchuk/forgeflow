package com.forgeflow.catalog.application;

import com.forgeflow.shared.api.OrganizationContext;
import com.forgeflow.shared.api.OrganizationId;
import java.util.Objects;

/**
 * Пример использования {@link OrganizationContext}: сервис получает текущую
 * организацию и явно передаёт её в repository-порт, не пробрасывая через
 * каждый метод.
 */
public class CatalogOrganizationScopeService {

    private final OrganizationContext organizationContext;
    private final OrganizationScopedRepository repository;

    public CatalogOrganizationScopeService(
            OrganizationContext organizationContext,
            OrganizationScopedRepository repository) {
        this.organizationContext = Objects.requireNonNull(organizationContext, "organizationContext must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public int countAssemblies() {
        OrganizationId organizationId = organizationContext.requiredOrganizationId();
        return repository.countByOrganization(organizationId);
    }
}
