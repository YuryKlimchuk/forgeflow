package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialJpaRepository extends JpaRepository<MaterialEntity, UUID> {

    Optional<MaterialEntity> findByOrganizationIdAndIdAndDeletedAtIsNull(UUID organizationId, UUID id);

    List<MaterialEntity> findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(UUID organizationId);

    boolean existsByOrganizationIdAndNumberAndDeletedAtIsNull(UUID organizationId, String number);
}
