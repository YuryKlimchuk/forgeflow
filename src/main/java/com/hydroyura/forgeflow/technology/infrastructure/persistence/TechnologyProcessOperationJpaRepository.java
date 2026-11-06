package com.hydroyura.forgeflow.technology.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnologyProcessOperationJpaRepository
        extends JpaRepository<TechnologyProcessOperationEntity, TechnologyProcessOperationId> {

    List<TechnologyProcessOperationEntity> findByProcessIdOrderByOperationNumberAsc(UUID processId);

    void deleteByProcessId(UUID processId);
}
