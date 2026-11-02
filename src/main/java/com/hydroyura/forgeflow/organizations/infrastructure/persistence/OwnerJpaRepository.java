package com.hydroyura.forgeflow.organizations.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerJpaRepository extends JpaRepository<OwnerEntity, UUID> {

    Optional<OwnerEntity> findByUserId(String userId);
}
