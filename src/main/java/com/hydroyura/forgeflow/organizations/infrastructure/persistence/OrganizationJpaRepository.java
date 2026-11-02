package com.hydroyura.forgeflow.organizations.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationEntity, UUID> {

    @Query("select o from OrganizationEntity o join o.memberUserIds uid where uid = :userId")
    Optional<OrganizationEntity> findByMemberUserId(@Param("userId") String userId);
}
