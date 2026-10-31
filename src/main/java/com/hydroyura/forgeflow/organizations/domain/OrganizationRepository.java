package com.hydroyura.forgeflow.organizations.domain;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.organizations.api.UserId;
import java.util.Optional;

/**
 * Driven port хранения организаций.
 */
public interface OrganizationRepository {

    void save(Organization organization);

    Optional<Organization> findById(OrganizationId id);

    Optional<Organization> findByMemberUserId(UserId userId);
}
