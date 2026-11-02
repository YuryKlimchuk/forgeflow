package com.hydroyura.forgeflow.organizations.infrastructure.persistence;

import com.hydroyura.forgeflow.organizations.api.OrganizationSettings;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.domain.Organization;
import com.hydroyura.forgeflow.organizations.domain.OrganizationRepository;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.Objects;
import java.util.Optional;

public class JpaOrganizationRepository implements OrganizationRepository {

    private final OrganizationJpaRepository repository;
    private final OrganizationMapper mapper = OrganizationMapper.INSTANCE;

    public JpaOrganizationRepository(OrganizationJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Organization organization) {
        repository.save(mapper.toEntity(organization));
    }

    @Override
    public Optional<Organization> findById(OrganizationId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Organization> findByMemberUserId(UserId userId) {
        return repository.findByMemberUserId(userId.value()).map(mapper::toDomain);
    }

    @Override
    public OrganizationSettings findSettings(OrganizationId id) {
        return repository.findById(id.value())
                .map(mapper::toSettings)
                .orElseGet(OrganizationSettings::empty);
    }
}
