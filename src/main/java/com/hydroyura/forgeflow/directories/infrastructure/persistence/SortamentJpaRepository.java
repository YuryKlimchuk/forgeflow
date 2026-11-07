package com.hydroyura.forgeflow.directories.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SortamentJpaRepository extends JpaRepository<SortamentEntity, UUID> {

    Optional<SortamentEntity> findByOrganizationIdAndIdAndDeletedAtIsNull(UUID organizationId, UUID id);

    List<SortamentEntity> findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(UUID organizationId);

    boolean existsByOrganizationIdAndNumberAndDeletedAtIsNull(UUID organizationId, String number);
}
