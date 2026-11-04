package com.hydroyura.forgeflow.technology.domain;

import com.hydroyura.forgeflow.catalog.api.PartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import com.hydroyura.forgeflow.technology.api.TechnologyProcessId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения технологических процессов.
 *
 * <p>Операции чтения принимают {@link OrganizationId} явным аргументом и не
 * извлекают организацию из глобального контекста. Удалённые (soft-deleted)
 * процессы не участвуют в чтении.</p>
 */
public interface TechnologyProcessRepository {

    void save(TechnologyProcess process);

    Optional<TechnologyProcess> findById(TechnologyProcessId id, OrganizationId organizationId);

    List<TechnologyProcess> findByPartId(PartId partId, OrganizationId organizationId);

    List<TechnologyProcess> findAll(OrganizationId organizationId);

    boolean existsByNumber(String number, OrganizationId organizationId);
}
