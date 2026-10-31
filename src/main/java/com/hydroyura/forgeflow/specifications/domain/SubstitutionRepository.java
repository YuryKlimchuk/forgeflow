package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.specifications.api.CatalogItemReference;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Optional;

/**
 * Driven port хранения доп. замен строк спецификации. Операции чтения и
 * удаления принимают {@link OrganizationId} явным аргументом.
 */
public interface SubstitutionRepository {

    void save(Substitution substitution);

    Optional<Substitution> findById(
            OrganizationId organizationId, AssemblyId assemblyId, CatalogItemReference item);

    void delete(OrganizationId organizationId, AssemblyId assemblyId, CatalogItemReference item);
}
