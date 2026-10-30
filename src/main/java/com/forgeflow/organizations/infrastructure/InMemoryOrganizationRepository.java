package com.forgeflow.organizations.infrastructure;

import com.forgeflow.shared.api.OrganizationId;
import com.forgeflow.organizations.api.UserId;
import com.forgeflow.organizations.domain.Organization;
import com.forgeflow.organizations.domain.OrganizationRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link OrganizationRepository}.
 */
public class InMemoryOrganizationRepository implements OrganizationRepository {

    private final Map<OrganizationId, Organization> byId = new ConcurrentHashMap<>();

    @Override
    public void save(Organization organization) {
        byId.put(organization.getId(), organization);
    }

    @Override
    public Optional<Organization> findById(OrganizationId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Organization> findByMemberUserId(UserId userId) {
        return byId.values().stream()
                .filter(organization -> organization.hasMember(userId))
                .findFirst();
    }
}
