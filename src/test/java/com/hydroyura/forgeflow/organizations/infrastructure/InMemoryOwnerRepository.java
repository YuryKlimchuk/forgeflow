package com.hydroyura.forgeflow.organizations.infrastructure;

import com.hydroyura.forgeflow.organizations.api.OwnerId;
import com.hydroyura.forgeflow.organizations.api.UserId;
import com.hydroyura.forgeflow.organizations.domain.Owner;
import com.hydroyura.forgeflow.organizations.domain.OwnerRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация {@link OwnerRepository}.
 */
public class InMemoryOwnerRepository implements OwnerRepository {

    private final Map<OwnerId, Owner> byId = new ConcurrentHashMap<>();
    private final Map<UserId, Owner> byUserId = new ConcurrentHashMap<>();

    @Override
    public void save(Owner owner) {
        byId.put(owner.getId(), owner);
        byUserId.put(owner.getUserId(), owner);
    }

    @Override
    public Optional<Owner> findById(OwnerId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Owner> findByUserId(UserId userId) {
        return Optional.ofNullable(byUserId.get(userId));
    }
}
