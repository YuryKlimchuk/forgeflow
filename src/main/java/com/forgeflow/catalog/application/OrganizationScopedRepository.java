package com.forgeflow.catalog.application;

import com.forgeflow.shared.api.OrganizationId;

/**
 * Пример repository-порта, ограничивающего операции организацией.
 *
 * <p>{@code OrganizationId} передаётся явным аргументом из application-сервиса,
 * а не читается из контекста внутри repository.</p>
 */
@FunctionalInterface
public interface OrganizationScopedRepository {

    int countByOrganization(OrganizationId organizationId);
}
