package com.hydroyura.forgeflow.organizations.infrastructure;

import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.domain.Organization;
import com.hydroyura.forgeflow.organizations.domain.OrganizationRepository;
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

    @Override
    public OrganizationSettings findSettings(OrganizationId id) {
        Organization organization = byId.get(id);
        return organization == null ? OrganizationSettings.empty() : organization.getSettings();
    }
}
