package com.hydroyura.forgeflow.catalog.domain.standart;

import com.hydroyura.forgeflow.catalog.api.StandartId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения стандартных изделий.
 *
 * <p>Операции чтения принимают {@link OrganizationId} явным аргументом и не
 * извлекают организацию из глобального контекста. Удалённые (soft-deleted)
 * стандартные изделия не участвуют в чтении.</p>
 */
public interface StandartRepository {

    void save(Standart standart);

    Optional<Standart> findById(StandartId id, OrganizationId organizationId);

    List<Standart> findAll(OrganizationId organizationId);

    boolean existsByNumber(String number, OrganizationId organizationId);
}
