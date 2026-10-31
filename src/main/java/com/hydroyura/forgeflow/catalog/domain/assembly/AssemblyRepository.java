package com.hydroyura.forgeflow.catalog.domain.assembly;

import com.hydroyura.forgeflow.catalog.api.AssemblyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения сборочных единиц.
 *
 * <p>Операции чтения принимают {@link OrganizationId} явным аргументом и не
 * извлекают организацию из глобального контекста. Удалённые (soft-deleted)
 * сборочные единицы не участвуют в чтении.</p>
 */
public interface AssemblyRepository {

    void save(Assembly assembly);

    Optional<Assembly> findById(AssemblyId id, OrganizationId organizationId);

    List<Assembly> findAll(OrganizationId organizationId);

    boolean existsByNumber(String number, OrganizationId organizationId);
}
