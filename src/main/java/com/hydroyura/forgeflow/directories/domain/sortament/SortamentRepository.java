package com.hydroyura.forgeflow.directories.domain.sortament;

import com.hydroyura.forgeflow.directories.api.SortamentId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения сортаментов.
 *
 * <p>Операции чтения принимают {@link OrganizationId} явным аргументом и не
 * извлекают организацию из глобального контекста. Удалённые (soft-deleted)
 * сортаменты не участвуют в чтении.</p>
 */
public interface SortamentRepository {

    void save(Sortament sortament);

    Optional<Sortament> findById(SortamentId id, OrganizationId organizationId);

    List<Sortament> findAll(OrganizationId organizationId);

    boolean existsByNumber(String number, OrganizationId organizationId);
}
