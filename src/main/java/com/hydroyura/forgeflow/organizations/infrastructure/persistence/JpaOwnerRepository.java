package com.hydroyura.forgeflow.organizations.infrastructure.persistence;

import com.hydroyura.forgeflow.organizations.api.OwnerId;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.domain.Owner;
import com.hydroyura.forgeflow.organizations.domain.OwnerRepository;
import java.util.Objects;
import java.util.Optional;

public class JpaOwnerRepository implements OwnerRepository {

    private final OwnerJpaRepository repository;
    private final OwnerMapper mapper = OwnerMapper.INSTANCE;

    public JpaOwnerRepository(OwnerJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void save(Owner owner) {
        repository.save(mapper.toEntity(owner));
    }

    @Override
    public Optional<Owner> findById(OwnerId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Owner> findByUserId(UserId userId) {
        return repository.findByUserId(userId.value()).map(mapper::toDomain);
    }
}
