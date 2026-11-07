package com.hydroyura.forgeflow.directories.domain.material;

import com.hydroyura.forgeflow.directories.api.MaterialId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения материалов.
 *
 * <p>Операции чтения принимают {@link OrganizationId} явным аргументом и не
 * извлекают организацию из глобального контекста. Удалённые (soft-deleted)
 * материалы не участвуют в чтении.</p>
 */
public interface MaterialRepository {

    void save(Material material);

    Optional<Material> findById(MaterialId id, OrganizationId organizationId);

    List<Material> findAll(OrganizationId organizationId);

    boolean existsByNumber(String number, OrganizationId organizationId);
}
