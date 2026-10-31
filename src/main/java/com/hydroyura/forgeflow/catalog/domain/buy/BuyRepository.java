package com.hydroyura.forgeflow.catalog.domain.buy;

import com.hydroyura.forgeflow.catalog.api.BuyId;
import com.hydroyura.forgeflow.shared.api.OrganizationId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port хранения покупных изделий.
 *
 * <p>Операции чтения принимают {@link OrganizationId} явным аргументом и не
 * извлекают организацию из глобального контекста. Удалённые (soft-deleted)
 * покупные изделия не участвуют в чтении.</p>
 */
public interface BuyRepository {

    void save(Buy buy);

    Optional<Buy> findById(BuyId id, OrganizationId organizationId);

    List<Buy> findAll(OrganizationId organizationId);

    boolean existsByNumber(String number, OrganizationId organizationId);
}
