package com.forgeflow.organizations.domain;

import com.forgeflow.organizations.api.OwnerId;
import com.forgeflow.organizations.api.UserId;
import java.util.Optional;

/**
 * Driven port хранения владельцев.
 */
public interface OwnerRepository {

    void save(Owner owner);

    Optional<Owner> findById(OwnerId id);

    Optional<Owner> findByUserId(UserId userId);
}
