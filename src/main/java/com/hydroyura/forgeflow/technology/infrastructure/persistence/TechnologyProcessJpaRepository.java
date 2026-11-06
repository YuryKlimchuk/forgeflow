package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnologyProcessJpaRepository extends JpaRepository<TechnologyProcessEntity, UUID> {

    Optional<TechnologyProcessEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

    List<TechnologyProcessEntity> findByOrganizationIdAndPartIdAndDeletedAtIsNullOrderByNumberAsc(
            UUID organizationId, UUID partId);

    List<TechnologyProcessEntity> findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(UUID organizationId);

    boolean existsByOrganizationIdAndNumberAndDeletedAtIsNull(UUID organizationId, String number);
}
