package com.forgeflow.catalog.domain.buy;

import com.forgeflow.catalog.api.BuyId;

/**
 * Driven port: сохранение и логическое удаление покупных изделий.
 */
public interface BuyRepository {

    void save(Buy buy);

    void delete(BuyId id);
}
