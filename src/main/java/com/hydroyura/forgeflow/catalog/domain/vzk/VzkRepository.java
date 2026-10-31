package com.hydroyura.forgeflow.catalog.domain.vzk;

import com.hydroyura.forgeflow.catalog.api.VzkId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения групп ВЗК.
 *
 * <p>Операции чтения принимают {@link OrganizationId} явным аргументом и не
 * извлекают организацию из глобального контекста. Удалённые (soft-deleted)
 * группы ВЗК не участвуют в чтении.</p>
 */
public interface VzkRepository {

    void save(Vzk vzk);

    Optional<Vzk> findById(VzkId id, OrganizationId organizationId);

    List<Vzk> findAll(OrganizationId organizationId);

    boolean existsByNumber(String number, OrganizationId organizationId);
}
