package com.hydroyura.forgeflow.planning.domain;

import com.hydroyura.forgeflow.planning.api.PlanId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения планов. Операции принимают {@link OrganizationId}
 * явным аргументом.
 */
public interface PlanRepository {

    void save(Plan plan);

    Optional<Plan> findById(OrganizationId organizationId, PlanId id);

    List<Plan> findAll(OrganizationId organizationId);

    boolean existsByName(OrganizationId organizationId, String name);
}
