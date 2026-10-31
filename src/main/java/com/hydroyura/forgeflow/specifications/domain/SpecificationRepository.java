package com.hydroyura.forgeflow.specifications.domain;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Optional;

/**
 * Driven port хранения спецификаций. Операции чтения принимают
 * {@link OrganizationId} явным аргументом.
 */
public interface SpecificationRepository {

    void save(Specification specification);

    Optional<Specification> findById(OrganizationId organizationId, AssemblyId assemblyId);
}
