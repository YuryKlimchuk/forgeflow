package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentJpaRepository extends JpaRepository<EquipmentEntity, UUID> {

    Optional<EquipmentEntity> findByOrganizationIdAndIdAndDeletedAtIsNull(UUID organizationId, UUID id);

    List<EquipmentEntity> findByOrganizationIdAndDeletedAtIsNullOrderByNumberAsc(UUID organizationId);

    boolean existsByOrganizationIdAndNumberAndDeletedAtIsNull(UUID organizationId, String number);
}
