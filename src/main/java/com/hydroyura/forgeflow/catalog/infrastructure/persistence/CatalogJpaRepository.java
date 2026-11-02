package com.hydroyura.forgeflow.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CatalogJpaRepository<T extends CatalogEntity> extends JpaRepository<T, UUID> {

    List<T> findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(UUID organizationId);

    Optional<T> findByOrganizationIdAndIdAndDeletedAtIsNull(UUID organizationId, UUID id);

    boolean existsByOrganizationIdAndNumberAndDeletedAtIsNull(UUID organizationId, String number);
}
