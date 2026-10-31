package com.forgeflow.catalog.domain.part;

import com.forgeflow.catalog.api.PartId;
import com.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения деталей.
 *
 * <p>Операции чтения принимают {@link OrganizationId} явным аргументом и не
 * извлекают организацию из глобального контекста. Удалённые (soft-deleted)
 * детали не участвуют в чтении.</p>
 */
public interface PartRepository {

    void save(Part part);

    Optional<Part> findById(PartId id, OrganizationId organizationId);

    List<Part> findAll(OrganizationId organizationId);

    boolean existsByNumber(String number, OrganizationId organizationId);
}
